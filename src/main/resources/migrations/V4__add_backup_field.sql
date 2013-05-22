ALTER TABLE `document`
ADD COLUMN `backup` INT(1) NULL;

UPDATE `document` SET `backup` = 0;