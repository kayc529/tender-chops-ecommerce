import { ECSClient, UpdateServiceCommand } from "@aws-sdk/client-ecs";

const ecs = new ECSClient({});

const cluster = process.env.ECS_CLUSTER;
const services = (process.env.ECS_SERVICES || "")
  .split(",")
  .map((s) => s.trim())
  .filter(Boolean);

export const handler = async () => {
  if (!cluster) {
    throw new Error("Missing ECS_CLUSTER environment variable");
  }

  if (services.length === 0) {
    throw new Error("Missing ECS_SERVICES environment variable");
  }

  const results = [];

  for (const service of services) {
    try {
      const response = await ecs.send(
        new UpdateServiceCommand({
          cluster,
          service,
          desiredCount: 0,
        }),
      );

      results.push({
        service,
        success: true,
        desiredCount: response.service?.desiredCount ?? 0,
        status: response.service?.status ?? "UNKNOWN",
      });
    } catch (error) {
      results.push({
        service,
        success: false,
        error: error.message,
      });
    }
  }

  return {
    cluster,
    results,
  };
};
