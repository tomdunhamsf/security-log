import { Agent, fetch as undiciFetch, type RequestInit as UndiciRequestInit } from 'undici';

const BACKEND = process.env.BACKEND_URL ?? 'https://localhost:8080';

// Reject self-signed certs unless BACKEND_TLS_VERIFY=false is set explicitly
const agent = new Agent({
  connect: {
    rejectUnauthorized: process.env.BACKEND_TLS_VERIFY !== 'false',
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
