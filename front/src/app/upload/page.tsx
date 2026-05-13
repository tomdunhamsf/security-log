'use client';

import { useState, useRef, ChangeEvent, DragEvent } from 'react';
import Link from 'next/link';
import { validateZscalerLogs } from '@/lib/zscaler-validator';

type UploadState = 'idle' | 'validating' | 'uploading' | 'success' | 'error';

interface ValidationSummary {
  valid: boolean;
  errorCount: number;
  warningCount: number;
  entryCount: number;
  errors: string[];
  warnings: string[];
}

export default function UploadPage() {
  const [file, setFile] = useState<File | null>(null);
  const [state, setState] = useState<UploadState>('idle');
  const [validation, setValidation] = useState<ValidationSummary | null>(null);
  const [serverMessage, setServerMessage] = useState('');
  const [dragging, setDragging] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  function selectFile(f: File) {
    setFile(f);
    setState('idle');
    setValidation(null);
    setServerMessage('');
  }

  function onFileChange(e: ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0];
    if (f) selectFile(f);
  }

  function onDrop(e: DragEvent) {
    e.preventDefault();
    setDragging(false);
    const f = e.dataTransfer.files?.[0];
    if (f) selectFile(f);
  }

  async function handleSubmit() {
    if (!file) return;
    setState('validating');
    setValidation(null);
    setServerMessage('');

    const text = await file.text();
    const result = validateZscalerLogs(text);

    const summary: ValidationSummary = {
      valid: result.valid,
      errorCount: result.errors.length,
      warningCount: result.warnings.length,
      entryCount: result.entries.length,
      errors: result.errors,
      warnings: result.warnings,
    };
    setValidation(summary);

    if (!result.valid) {
      setState('error');
      return;
    }

    setState('uploading');
    try {
      const form = new FormData();
      form.append('file', file);

      const res = await fetch('/api/ingest', { method: 'POST', body: form });
      const data = await res.json().catch(() => ({}));

      if (!res.ok) {
        setServerMessage(data.error ?? `Server returned ${res.status}.`);
        setState('error');
      } else {
        setServerMessage(data.message ?? 'Log file ingested successfully.');
        setState('success');
        setFile(null);
        if (inputRef.current) inputRef.current.value = '';
      }
    } catch {
      setServerMessage('Network error — could not reach the server.');
      setState('error');
    }
  }

  return (
    <div className="min-h-screen flex flex-col">
      <header className="bg-white border-b border-gray-200 px-6 py-4 flex items-center gap-4">
        <Link href="/dashboard" className="text-sm text-blue-600 hover:underline">
          ← Dashboard
        </Link>
        <h1 className="text-lg font-semibold text-gray-800">Upload Log File</h1>
      </header>

      <main className="flex-1 flex flex-col items-center justify-center px-4 py-12">
        <div className="w-full max-w-lg space-y-6">
          {/* Drop zone */}
          <div
            onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
            onDragLeave={() => setDragging(false)}
            onDrop={onDrop}
            onClick={() => inputRef.current?.click()}
            className={`cursor-pointer rounded-2xl border-2 border-dashed flex flex-col items-center justify-center gap-3 py-14 transition-colors ${
              dragging ? 'border-blue-400 bg-blue-50' : 'border-gray-300 bg-white hover:border-blue-400 hover:bg-blue-50'
            }`}
          >
            <span className="text-4xl">📂</span>
            {file ? (
              <div className="text-center">
                <p className="text-sm font-semibold text-gray-800">{file.name}</p>
                <p className="text-xs text-gray-400">{(file.size / 1024).toFixed(1)} KB</p>
              </div>
            ) : (
              <div className="text-center">
                <p className="text-sm font-medium text-gray-600">Drag &amp; drop a log file here</p>
                <p className="text-xs text-gray-400">or click to browse</p>
              </div>
            )}
            <input
              ref={inputRef}
              type="file"
              accept=".log,.txt"
              onChange={onFileChange}
              className="hidden"
            />
          </div>

          {/* Validation result */}
          {validation && (
            <div
              className={`rounded-xl border px-4 py-4 text-sm space-y-2 ${
                validation.valid
                  ? 'border-green-200 bg-green-50'
                  : 'border-red-200 bg-red-50'
              }`}
            >
              <p className={`font-semibold ${validation.valid ? 'text-green-700' : 'text-red-700'}`}>
                {validation.valid
                  ? `✓ Valid — ${validation.entryCount} log entries detected`
                  : `✗ Validation failed — ${validation.errorCount} error${validation.errorCount !== 1 ? 's' : ''}`}
              </p>

              {validation.errors.map((e, i) => (
                <p key={i} className="text-red-600 text-xs">• {e}</p>
              ))}

              {validation.warningCount > 0 && (
                <details className="text-xs">
                  <summary className="cursor-pointer text-amber-600 font-medium">
                    {validation.warningCount} warning{validation.warningCount !== 1 ? 's' : ''}
                  </summary>
                  <div className="mt-1 space-y-1">
                    {validation.warnings.map((w, i) => (
                      <p key={i} className="text-amber-700">• {w}</p>
                    ))}
                  </div>
                </details>
              )}
            </div>
          )}

          {/* Server message */}
          {serverMessage && (
            <div
              className={`rounded-xl border px-4 py-3 text-sm ${
                state === 'success'
                  ? 'border-green-200 bg-green-50 text-green-700'
                  : 'border-red-200 bg-red-50 text-red-700'
              }`}
            >
              {serverMessage}
            </div>
          )}

          <button
            onClick={handleSubmit}
            disabled={!file || state === 'validating' || state === 'uploading'}
            className="w-full rounded-lg bg-blue-600 py-2.5 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50 transition-colors"
          >
            {state === 'validating' && 'Validating…'}
            {state === 'uploading' && 'Uploading…'}
            {(state === 'idle' || state === 'error' || state === 'success') && 'Validate & Upload'}
          </button>

          <p className="text-xs text-center text-gray-400">
            Only Zscaler NSS web proxy log format is accepted.{' '}
            <a
              href="https://help.zscaler.com/zia/nss-feed-output-format-web-logs"
              target="_blank"
              rel="noopener noreferrer"
              className="underline hover:text-blue-500"
            >
              Format reference
            </a>
          </p>
        </div>
      </main>
    </div>
  );
}
