import {
  S3Client,
  GetObjectCommand,
  PutObjectCommand,
} from "@aws-sdk/client-s3";
import { Readable } from "stream";
import sharp from "sharp";
import {
  SecretsManagerClient,
  GetSecretValueCommand,
} from "@aws-sdk/client-secrets-manager";

const s3 = new S3Client({});

const secretsClient = new SecretsManagerClient({});
let cachedInternalToken = null;

export async function getInternalToken() {
  if (cachedInternalToken) {
    return cachedInternalToken;
  }

  const secretId = process.env.PRODUCT_IMAGE_CALLBACK_SECRET_NAME;
  if (!secretId) {
    throw new Error("Missing PRODUCT_IMAGE_CALLBACK_SECRET_NAME");
  }

  const response = await secretsClient.send(
    new GetSecretValueCommand({
      SecretId: secretId,
    }),
  );

  if (!response.SecretString) {
    throw new Error("SecretString is empty");
  }

  // If you store plain text:
  cachedInternalToken = response.SecretString;

  return cachedInternalToken;
}

export const handler = async (event, context) => {
  //  get key from event object
  const originalBucket = event.Records[0].s3.bucket.name;
  let originalKey = event.Records[0].s3.object.key;
  originalKey = decodeURIComponent(originalKey.replace(/\+/g, " "));

  //  validate key structure first so callback payload fields are trustworthy
  const keyParts = originalKey.split("/");

  // expected format: products/{productId}/original/{filename}
  if (keyParts.length !== 4 || keyParts[2] !== "original") {
    console.log(
      `Invalid S3 key format. bucket=${originalBucket}, key=${originalKey}, expectedFormat=products/{productId}/original/{filename}`,
    );
    throw new Error(
      `Invalid S3 key format. bucket=${originalBucket}, key=${originalKey}, expectedFormat=products/{productId}/original/{filename}`,
    );
  }

  const productId = keyParts[1];
  const filename = keyParts[3];
  const thumbnailKey = `products/${productId}/thumbnails/${filename}`;

  //  validate the format of the original image
  // Infer the image type from the file suffix
  const typeMatch = originalKey.match(/\.([^.]*)$/);
  if (!typeMatch) {
    console.log(
      `Could not determine image type from key. bucket=${originalBucket}, key=${originalKey}`,
    );
    await sendImageProcessingResultCallback({
      productId,
      imageKey: originalKey,
      thumbnailKey,
      success: false,
      failureReason: "Could not determine image type from key.",
    });
    return;
  }

  // Check that the image type is supported
  const imageType = typeMatch[1].toLowerCase();
  if (imageType != "jpg" && imageType != "png") {
    console.log(
      `Unsupported image type from key suffix. bucket=${originalBucket}, key=${originalKey}, imageType=${imageType}`,
    );
    await sendImageProcessingResultCallback({
      productId,
      imageKey: originalKey,
      thumbnailKey,
      success: false,
      failureReason: `Unsupported image type from key suffix: ${imageType}.`,
    });
    return;
  }

  //  get the original image with the original key
  let contentBuffer;

  try {
    const params = {
      Bucket: originalBucket,
      Key: originalKey,
    };
    let response = await s3.send(new GetObjectCommand(params));
    const stream = response.Body;

    // Convert stream to buffer to pass to sharp resize function.
    if (stream instanceof Readable) {
      contentBuffer = Buffer.concat(await stream.toArray());

      const expectedFormat = imageType === "jpg" ? "jpeg" : "png";
      const metadata = await sharp(contentBuffer).metadata();

      if (metadata.format !== expectedFormat) {
        console.log(
          `Image format mismatch. bucket=${originalBucket}, key=${originalKey}, expectedFormat=${expectedFormat}, actualFormat=${metadata.format}`,
        );
        await sendImageProcessingResultCallback({
          productId,
          imageKey: originalKey,
          thumbnailKey,
          success: false,
          failureReason: `Image format mismatch. Expected ${expectedFormat} but got ${metadata.format}.`,
        });
        return;
      }
    } else {
      throw new Error("Unknown object stream type");
    }
  } catch (error) {
    console.log(
      `Failed to get original image from S3. bucket=${originalBucket}, key=${originalKey}, error=${error?.message}`,
      error,
    );
    throw new Error("Failed to get original image from S3");
  }

  //  resize image and create thumbnail image
  const width = parseInt(process.env.THUMBNAIL_SIZE_WIDTH, 10);
  let outputBuffer;
  try {
    outputBuffer = await sharp(contentBuffer).resize(width).toBuffer();
  } catch (error) {
    console.log(
      `Failed to resize original image. bucket=${originalBucket}, key=${originalKey}, error=${error?.message}`,
      error,
    );
    throw new Error("Failed to resize original image");
  }

  //  upload thumbnail image to thumbnail bucket
  const thumbnailBucket = process.env.THUMBNAIL_BUCKET_NAME;
  const contentType = imageType === "jpg" ? "image/jpeg" : "image/png";
  try {
    const destparams = {
      Bucket: thumbnailBucket,
      Key: thumbnailKey,
      Body: outputBuffer,
      ContentType: contentType,
    };

    await s3.send(new PutObjectCommand(destparams));
    console.log(
      `Uploaded thumbnail image successfully. bucket=${thumbnailBucket}, key=${thumbnailKey}, sourceKey=${originalKey}`,
    );
  } catch (error) {
    console.log(
      `Failed to upload thumbnail image to S3. bucket=${thumbnailBucket}, key=${thumbnailKey}, sourceKey=${originalKey}, error=${error?.message}`,
      error,
    );
    throw new Error("Failed to upload thumbnail image to S3");
  }

  console.log(
    `Thumbnail generation completed successfully. productId=${productId}, originalBucket=${originalBucket}, originalKey=${originalKey}, thumbnailBucket=${thumbnailBucket}, thumbnailKey=${thumbnailKey}`,
  );

  await sendImageProcessingResultCallback({
    productId,
    imageKey: originalKey,
    thumbnailKey,
    success: true,
    failureReason: null,
  });
};

const sendImageProcessingResultCallback = async (payload) => {
  const baseUrl = process.env.PRODUCT_SERVICE_BASE_URL;
  if (!baseUrl) {
    throw new Error("Missing PRODUCT_SERVICE_BASE_URL");
  }

  const internalToken = await getInternalToken();
  const callbackUrl = `${baseUrl}/internal/product-images/result`;

  const response = await fetch(callbackUrl, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Internal-Token": internalToken,
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const responseBody = await response.text();
    console.log(
      `Product-service callback failed. callbackUrl=${callbackUrl}, status=${response.status}, statusText=${response.statusText}, payload=${JSON.stringify(payload)}, responseBody=${responseBody}`,
    );
    throw new Error("Product-service callback failed");
  }

  console.log(
    `Product-service callback succeeded. callbackUrl=${callbackUrl}, payload=${JSON.stringify(payload)}`,
  );
};
