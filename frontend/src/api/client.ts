export interface SecurityState {
  configured: boolean;
  encryptionSalt: string;
  authSalt: string;
}

export interface CameraConfiguration {
  id: number;
  deviceId: string;
  label: string;
  direction: 'ENTRY' | 'EXIT';
  updatedAt: string;
}

export interface Person {
  id: number;
  publicId: string;
  faceCiphertext: string;
  faceIv: string;
  profileCiphertext?: string | null;
  profileIv?: string | null;
  consentAt: string;
  createdAt: string;
  lastRecognizedAt?: string | null;
}

export interface EventRecord {
  id: number;
  personId: number;
  publicId: string;
  recognizedAt: string;
  cameraId?: number | null;
  cameraLabel?: string | null;
  direction?: 'ENTRY' | 'EXIT' | null;
}

export interface RecognitionReceipt extends EventRecord { created: boolean }
export interface Profile { fullName: string; email?: string; phone?: string }
export interface EncryptedPayload { ciphertext: string; iv: string }

const API_ROOT = '/api';

async function request<T>(path: string, token?: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body !== undefined && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  if (token) headers.set('Authorization', `Bearer ${token}`);
  const response = await fetch(`${API_ROOT}${path}`, { ...init, headers });
  if (!response.ok) {
    const message = await response.text().catch(() => '');
    throw new Error(message || `Falha na API (${response.status})`);
  }
  if (response.status === 204) return undefined as T;
  const body = await response.text();
  return (body ? JSON.parse(body) : undefined) as T;
}

export const api = {
  securityState: () => request<SecurityState>('/security/state'),
  setup: (token: string, bootstrapKey: string) => request<void>('/security/setup', undefined, { method: 'POST', body: JSON.stringify({ token, bootstrapKey }) }),
  login: (token: string) => request<void>('/security/login', undefined, { method: 'POST', body: JSON.stringify({ token }) }),
  people: (token: string) => request<Person[]>('/people', token),
  cameras: (token: string) => request<CameraConfiguration[]>('/cameras', token),
  saveCameras: (token: string, cameras: Array<Pick<CameraConfiguration, 'deviceId' | 'label' | 'direction'>>) =>
    request<CameraConfiguration[]>('/cameras', token, { method: 'PUT', body: JSON.stringify({ cameras }) }),
  createPerson: (token: string, payload: { faceCiphertext: string; faceIv: string; consentConfirmed: true; cameraId: number }) =>
    request<Person>('/people', token, { method: 'POST', body: JSON.stringify(payload) }),
  saveProfile: (token: string, id: number, payload: EncryptedPayload) =>
    request<Person>(`/people/${id}/profile`, token, { method: 'PUT', body: JSON.stringify({ profileCiphertext: payload.ciphertext, profileIv: payload.iv }) }),
  deletePerson: (token: string, id: number) => request<void>(`/people/${id}`, token, { method: 'DELETE' }),
  createEvent: (token: string, id: number, cameraId: number) =>
    request<RecognitionReceipt>(`/people/${id}/events`, token, { method: 'POST', body: JSON.stringify({ cameraId }) }),
  events: (token: string) => request<EventRecord[]>('/events?limit=200', token),
};
