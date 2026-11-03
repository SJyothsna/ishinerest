-- Staging for legacy evidence with unknown historical revisions. Never included in new metrics.
CREATE TABLE imported_evidence(import_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),source_table text NOT NULL,payload jsonb NOT NULL,imported_at timestamptz NOT NULL DEFAULT now());
