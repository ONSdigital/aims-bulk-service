ALTER TABLE bulkinfo ADD COLUMN dataset VARCHAR(255);
ALTER TABLE bulkinfo ADD COLUMN topic VARCHAR(255);
ALTER TABLE bulkinfo ADD COLUMN uimetadata VARCHAR(10000);

ALTER TABLE ids_bulkinfo ADD COLUMN dataset VARCHAR(255);
ALTER TABLE ids_bulkinfo ADD COLUMN topic VARCHAR(255);
ALTER TABLE ids_bulkinfo ADD COLUMN uimetadata VARCHAR(10000);
