'use client';

import { useEffect, useState, use } from 'react';
import Link from 'next/link';
import { PREFERRED_COLUMNS, type ZscalerLogEntry } from '@/lib/zscaler-validator';

// Parse raw Zscaler NSS log text into structured entries
function parseRawLog(text: string): ZscalerLogEntry[] {
  const KV_REGEX = /(\w+)=("(?:[^"\\]|\\.)*"|[^\s]+)/g;
  return text
    .split('\n')
    .filter((l) => l.trim())
    .map((line) => {
      const entry: ZscalerLogEntry = {};
      let m: RegExpExecArray | null;
      KV_REGEX.lastIndex = 0;
      while ((m = KV_REGEX.exec(line)) !== null) {
        const key = m[1].toLowerCase();
        const raw = m[2];
        entry[key] = raw.startsWith('"') ? raw.slice(1, -1) : raw;
      }
      return entry;
    })
    .filter((e) => Object.keys(e).length > 0);
}

function deriveColumns(entries: ZscalerLogEntry[]): string[] {
  const allKeys = new Set<string>();
  entries.forEach((e) => Object.keys(e).forEach((k) => allKeys.add(k)));

  // Preferred columns first, then any extras alphabetically
  const ordered = PREFERRED_COLUMNS.filter((c) => allKeys.has(c));
  const extras = [...allKeys]
    .filter((k) => !PREFERRED_COLUMNS.includes(k))
    .sort();
  return [...ordered, ...extras];
}

interface AnalysisResult {
  description: string;
  certainty: number;
  rows: number[];
}

export default function LogViewerPage({ params }: { params: Promise<{ filename: string }> }) {
  const { filename } = use(params);
  const decoded = decodeURIComponent(filename);

  const [entries, setEntries] = useState<ZscalerLogEntry[]>([]);
  const [columns, setColumns] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [analyzing, setAnalyzing] = useState(false);
  const [analysis, setAnalysis] = useState<AnalysisResult | null>(null);
  const [analyzeError, setAnalyzeError] = useState('');

  useEffect(() => {
    async function load() {
      try {
        const res = await fetch(`/api/display/${encodeURIComponent(decoded)}`);
        if (!res.ok) throw new Error(`Server returned ${res.status}`);

        const contentType = res.headers.get('content-type') ?? '';
        let parsed: ZscalerLogEntry[] = [];

        if (contentType.includes('application/json')) {
          const data = await res.json();
          // Accept array or { entries: [...] } or { logs: [...] }
          parsed = Array.isArray(data)
            ? data
            : (data.entries ?? data.logs ?? data.data ?? []);
        } else {
          const text = await res.text();
          parsed = parseRawLog(text);
        }

        setEntries(parsed);
        setColumns(deriveColumns(parsed));
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load log file.');
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [decoded]);

  async function runAnalysis() {
    setAnalyzing(true);
    setAnalyzeError('');
    setAnalysis(null);
    try {
      const res = await fetch(`/api/display/${encodeURIComponent(decoded)}/analyze`, {
        method: 'POST',
      });
      if (!res.ok) throw new Error(`Server returned ${res.status}`);
      const data: AnalysisResult = await res.json();
      if (data.certainty > 0) {
        setAnalysis(data);
      }
    } catch (err) {
      setAnalyzeError(err instanceof Error ? err.message : 'Analysis failed.');
    } finally {
      setAnalyzing(false);
    }
  }

  const threatRows = new Set(analysis?.rows ?? []);

  return (
    <div className="min-h-screen flex flex-col">
      <header className="bg-white border-b border-gray-200 px-6 py-4 flex items-center gap-4">
        <Link href="/logs" className="text-sm text-blue-600 hover:underline">
          ← Log Files
        </Link>
        <h1 className="text-lg font-semibold text-gray-800 truncate">{decoded}</h1>
        {!loading && !error && (
          <>
            <span className="text-xs text-gray-400">{entries.length} entries</span>
            <button
              onClick={runAnalysis}
              disabled={analyzing}
              className="ml-auto px-4 py-1.5 rounded-lg bg-blue-600 text-white text-sm font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {analyzing ? 'Analyzing…' : 'Analyze'}
            </button>
          </>
        )}
      </header>

      <main className="flex-1 overflow-auto px-2 py-4">
        {loading && (
          <p className="text-sm text-gray-500 text-center mt-12">Loading…</p>
        )}

        {error && (
          <div className="mx-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </div>
        )}

        {analyzeError && (
          <div className="mx-4 mb-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {analyzeError}
          </div>
        )}

        {analysis && (
          <div className="mx-4 mb-4 rounded-xl border border-orange-300 bg-orange-50 px-4 py-3 flex items-center gap-4">
            <span className="text-sm font-semibold text-orange-800">Threat detected:</span>
            <span className="text-sm text-orange-900 flex-1">{analysis.description}</span>
            <span className="text-sm font-bold text-orange-700 whitespace-nowrap">{analysis.certainty}% certainty</span>
          </div>
        )}

        {!loading && !error && entries.length === 0 && (
          <p className="text-sm text-gray-500 text-center mt-12">No parseable log entries found.</p>
        )}

        {!loading && entries.length > 0 && (
          <div className="overflow-x-auto rounded-xl border border-gray-200 shadow-sm">
            <table className="min-w-full text-xs divide-y divide-gray-200">
              <thead className="bg-gray-100 sticky top-0">
                <tr>
                  {columns.map((col) => (
                    <th
                      key={col}
                      className="px-3 py-2 text-left font-semibold text-gray-600 uppercase tracking-wide whitespace-nowrap"
                    >
                      {col}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-100">
                {entries.map((entry, i) => (
                  <tr
                    key={i}
                    style={threatRows.has(i) ? { backgroundColor: '#fee2e2' } : undefined}
                    className={threatRows.has(i) ? undefined : i % 2 === 0 ? 'bg-white' : 'bg-gray-50'}
                  >
                    {columns.map((col) => (
                      <td
                        key={col}
                        title={entry[col] ?? ''}
                        className="px-3 py-2 text-gray-700 max-w-xs truncate whitespace-nowrap"
                      >
                        {entry[col] ?? ''}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>
    </div>
  );
}
