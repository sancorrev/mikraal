import { NextResponse, type NextRequest } from "next/server";

const SESSION_COOKIE = "MIKRAAL_SESSION";

/**
 * Only for `pnpm dev:mock`: the Prism mock never sets the session cookie,
 * so every API call would get 401. With MIKRAAL_MOCK_SESSION=1 a fake
 * cookie is added to API requests that have none. The real BFF ignores it.
 */
export function proxy(request: NextRequest) {
  if (process.env.MIKRAAL_MOCK_SESSION !== "1" || request.cookies.has(SESSION_COOKIE)) {
    return NextResponse.next();
  }
  const headers = new Headers(request.headers);
  const cookies = [headers.get("cookie"), `${SESSION_COOKIE}=mock`].filter(Boolean);
  headers.set("cookie", cookies.join("; "));
  return NextResponse.next({ request: { headers } });
}

export const config = { matcher: "/api/:path*" };
