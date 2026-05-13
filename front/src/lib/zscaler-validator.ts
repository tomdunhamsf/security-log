export interface ZscalerLogEntry {
  [key: string]: string;
}

export interface ValidationResult {
  valid: boolean;
  errors: string[];
  warnings: string[];
  entries: ZscalerLogEntry[];
}

// Fields that must be present on every log line
const REQUIRED_FIELDS = ['action', 'reason', 'username', 'url', 'status'];

// Fields unique to Zscaler NSS — at least one must appear per line
const DISTINCTIVE_FIELDS = [
  'sourcetype',
  'transactionid',
  'urlcategory',
  'urlcat',
  'urlsupercategory',
  'urlclass',
  'malwareclass',
  'realm',
  'reqsize',
  'respsize',
];

const VALID_ACTIONS = ['allowed', 'blocked', 'caution', 'isolated', 'allow', 'block'];

// Matches key=value and key="quoted value" pairs
const KV_REGEX = /(\w+)=("(?:[^"\\]|\\.)*"|[^\s]+)/g;

// Leading syslog-style timestamp: Mon DD HH:MM:SS YYYY
const TIMESTAMP_REGEX =
  /^(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s+\d{1,2}\s+\d{2}:\d{2}:\d{2}\s+\d{4}\s/;

function parseLine(line: string): ZscalerLogEntry {
  const entry: ZscalerLogEntry = {};
  let match: RegExpExecArray | null;
  KV_REGEX.lastIndex = 0;
  while ((match = KV_REGEX.exec(line)) !== null) {
    const key = match[1].toLowerCase();
    const raw = match[2];
    entry[key] = raw.startsWith('"') ? raw.slice(1, -1) : raw;
  }
  return entry;
}

export function validateZscalerLogs(content: string): ValidationResult {
  const errors: string[] = [];
  const warnings: string[] = [];
  const entries: ZscalerLogEntry[] = [];

  const lines = content.split('\n').filter((l) => l.trim().length > 0);

  if (lines.length === 0) {
    return { valid: false, errors: ['File is empty.'], warnings: [], entries: [] };
  }

  let validCount = 0;

  for (let i = 0; i < lines.length; i++) {
    const lineNum = i + 1;
    const line = lines[i];

    if (!TIMESTAMP_REGEX.test(line)) {
      warnings.push(`Line ${lineNum}: Missing expected syslog timestamp prefix.`);
    }

    const entry = parseLine(line);

    if (Object.keys(entry).length === 0) {
      errors.push(`Line ${lineNum}: No key=value pairs found.`);
      continue;
    }

    const missing = REQUIRED_FIELDS.filter((f) => !(f in entry));
    if (missing.length > 0) {
      errors.push(`Line ${lineNum}: Missing required fields: ${missing.join(', ')}.`);
      continue;
    }

    if (!VALID_ACTIONS.includes(entry.action.toLowerCase())) {
      warnings.push(
        `Line ${lineNum}: Unexpected action value "${entry.action}". Expected: Allowed, Blocked, Caution, or Isolated.`
      );
    }

    const statusCode = parseInt(entry.status, 10);
    if (isNaN(statusCode) || statusCode < 100 || statusCode > 599) {
      warnings.push(`Line ${lineNum}: Invalid HTTP status code "${entry.status}".`);
    }

    const hasDistinctive = DISTINCTIVE_FIELDS.some((f) => f in entry);
    if (!hasDistinctive) {
      warnings.push(
        `Line ${lineNum}: No Zscaler-distinctive fields found (e.g., sourcetype, transactionid, urlcategory).`
      );
    }

    validCount++;
    entries.push(entry);
  }

  if (validCount === 0) {
    errors.push('No valid Zscaler NSS log entries found in the file.');
  } else if (validCount < lines.length * 0.5) {
    errors.push(
      `Only ${validCount} of ${lines.length} lines are valid Zscaler NSS entries (need ≥50%).`
    );
  }

  return { valid: errors.length === 0, errors, warnings, entries };
}

// Ordered list of columns preferred for table display
export const PREFERRED_COLUMNS = [
  'action',
  'reason',
  'username',
  'url',
  'status',
  'urlcategory',
  'urlcat',
  'reqmethod',
  'contenttype',
  'serverip',
  'clientip',
  'duration',
  'reqsize',
  'respsize',
  'malwareclass',
  'malwarename',
  'filetype',
];
