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
    // Stream the raw body directly — parsing and re-serialising FormData would
    // drop the original boundary from the Content-Type, breaking Tomcat's
    // multipart parser.
    const upstream = await backendFetch('/ingest', {
      method: 'POST',
      headers: { 'Content-Type': contentType },
      body: req.body as unknown as BodyInit,
      token,
    });

    const data = await upstream.json().catch(() => ({}));
    return NextResponse.json(data, { status: upstream.status });
  } catch {
    return NextResponse.json({ error: 'Internal server error.' }, { status: 500 });
  }
}
