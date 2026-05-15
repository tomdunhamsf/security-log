import { NextRequest, NextResponse } from 'next/server';
import { backendFetch } from '@/lib/backend';

export async function POST(req: NextRequest) {
  const token = req.cookies.get('auth_token')?.value;
  if (!token) {
    return NextResponse.json({ error: 'Not authenticated.' }, { status: 401 });
  }

  const contentType = req.headers.get('content-type') ?? '';
  if (!contentType.includes('multipart/form-data')) {
    return NextResponse.json({ error: 'Expected multipart/form-data.' }, { status: 400 });
  }

  try {
    // Read the full body into memory so undici sends it with a known
    // Content-Length, which Tomcat's multipart parser requires.
    const body = await req.arrayBuffer();

    const upstream = await backendFetch('/ingest', {
      method: 'POST',
      headers: { 'Content-Type': contentType },
      body,
      token,
    });

    const data = await upstream.json().catch(() => ({}));
    return NextResponse.json(data, { status: upstream.status });
  } catch (err) {
    console.error('[ingest proxy] failed to reach backend:', err);
    return NextResponse.json({ error: 'Internal server error.' }, { status: 500 });
  }
}
