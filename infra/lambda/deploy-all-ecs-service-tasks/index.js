import {
  ECSClient,
  DescribeServicesCommand,
  UpdateServiceCommand,
} from "@aws-sdk/client-ecs";

const ecs = new ECSClient({});

const cluster = process.env.ECS_CLUSTER;
const services = (process.env.ECS_SERVICES || "")
  .split(",")
  .map((s) => s.trim())
  .filter(Boolean);

function extractTaskDefinitionFamily(taskDefinitionArn) {
  if (!taskDefinitionArn) {
    throw new Error("Service does not have a task definition");
  }

  const taskDefinitionName = taskDefinitionArn.split("/").pop(); // family:revision
  const family = taskDefinitionName?.split(":")[0];

  if (!family) {
    throw new Error(
      `Unable to extract task definition family from: ${taskDefinitionArn}`,
    );
  }

  return family;
}

export const handler = async () => {
  if (!cluster) {
    throw new Error("Missing ECS_CLUSTER environment variable");
  }

  if (services.length === 0) {
    throw new Error("Missing ECS_SERVICES environment variable");
  }

  const results = [];

  for (const serviceName of services) {
    try {
      const describeResponse = await ecs.send(
        new DescribeServicesCommand({
          cluster,
          services: [serviceName],
        }),
      );

      const service = describeResponse.services?.[0];

      if (!service) {
        throw new Error(`Service not found: ${serviceName}`);
      }

      const family = extractTaskDefinitionFamily(service.taskDefinition);

      const updateResponse = await ecs.send(
        new UpdateServiceCommand({
          cluster,
          service: serviceName,
          taskDefinition: family,
          desiredCount: 1,
        }),
      );

      results.push({
        service: serviceName,
        success: true,
        taskDefinitionFamily: family,
        taskDefinition: updateResponse.service?.taskDefinition ?? "UNKNOWN",
        desiredCount: updateResponse.service?.desiredCount ?? 1,
        status: updateResponse.service?.status ?? "UNKNOWN",
      });
    } catch (error) {
      results.push({
        service: serviceName,
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
