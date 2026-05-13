import { Agent, fetch as undiciFetch, type RequestInit as UndiciRequestInit } from 'undici';

const BACKEND = 'https://localhost:8080';

// Accept self-signed certs in dev; enforce in production
const agent = new Agent({
  connect: {
    rejectUnauthorized: process.env.NODE_ENV === 'production',
  },
});

export async function backendFetch(
  path: string,
  options: RequestInit & { token?: string } = {}
): Promise<Response> {
  const { token, headers: extraHeaders, ...rest } = options;

  const headers: Record<string, string> = {
    ...(extraHeaders as Record<string, string>),
  };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const undiciOptions: UndiciRequestInit = {
    ...(rest as UndiciRequestInit),
    headers,
    dispatcher: agent,
  };

  const response = await undiciFetch(`${BACKEND}${path}`, undiciOptions);
  return response as unknown as Response;
}
