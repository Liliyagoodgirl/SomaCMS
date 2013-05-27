ALTER TABLE `document_data`
ADD `document_version` int(2) NOT NULL,
ADD PRIMARY KEY (`document_version`),
DROP FOREIGN KEY `document_data_ibfk_1`,
ADD CONSTRAINT `FK_document_version`
FOREIGN KEY (`document_id`, `document_version`) REFERENCES `document_version` (`document_id`, `document_version`) ON DELETE CASCADE

