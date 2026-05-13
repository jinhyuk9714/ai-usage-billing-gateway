import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 5,
  duration: "30s",
  thresholds: {
    http_req_failed: ["rate<0.05"],
  },
};

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const apiKey = __ENV.API_KEY || "replace-with-created-api-key";

export default function () {
  const gatewayResponse = http.post(
    `${baseUrl}/v1/gateway/mock-completion`,
    JSON.stringify({ prompt: "portfolio load probe" }),
    {
      headers: {
        "Content-Type": "application/json",
        "X-API-Key": apiKey,
      },
    },
  );

  check(gatewayResponse, {
    "gateway returned non-5xx": (response) => response.status < 500,
  });

  const usageResponse = http.post(
    `${baseUrl}/api/usage/events`,
    JSON.stringify({ metric: "REQUEST", quantity: 1, metadata: { source: "k6" } }),
    {
      headers: {
        "Content-Type": "application/json",
        "X-API-Key": apiKey,
        "Idempotency-Key": `k6-${__VU}-${__ITER}`,
      },
    },
  );

  check(usageResponse, {
    "usage returned non-5xx": (response) => response.status < 500,
  });

  sleep(1);
}
