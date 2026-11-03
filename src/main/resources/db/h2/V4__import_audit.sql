CREATE TABLE imported_evidence(import_id uuid DEFAULT random_uuid() PRIMARY KEY,source_table text NOT NULL,payload text NOT NULL,imported_at timestamp with time zone NOT NULL DEFAULT now());
