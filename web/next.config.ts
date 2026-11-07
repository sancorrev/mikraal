import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

const bffUrl = process.env.BFF_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  devIndicators: false,
  // Do not write AGENTS.md into the project on `next dev`.
  agentRules: false,
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${bffUrl}/api/:path*` }];
  },
};

export default createNextIntlPlugin("./i18n/request.ts")(nextConfig);
