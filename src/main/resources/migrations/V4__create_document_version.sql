CREATE TABLE `document_version` (
  `document_id` int(11) NOT NULL,
  `document_version` int(2) NOT NULL,
  `creation_time` timestamp,
  PRIMARY KEY (`document_id`,`document_version`),
  FOREIGN KEY (`document_id`) REFERENCES `document` (`id`) ON DELETE CASCADE
)