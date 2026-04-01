export interface Customer {
  id: number;
  externalId: string;
  name: string;
  email: string | null;
  phone: string | null;
  address: string | null;
  sourceSystem: string;
  firstSyncedAt: string;
  lastSyncedAt: string;
}
