-- Written for MySQL

--DROP TABLE IF EXISTS `music`;
--DROP TABLE IF EXISTS `music_tag`;
--DROP TABLE IF EXISTS `artwork`;
--DROP TABLE IF EXISTS `music_artwork`;

--DROP TABLE IF EXISTS `text`;
--DROP TABLE IF EXISTS `text_link`;
--DROP TABLE IF EXISTS `text_tag`;

--DROP TABLE IF EXISTS `document`;

--DROP TABLE IF EXISTS `cache`;

--DROP TABLE IF EXISTS `request`;
--DROP TABLE IF EXISTS `track`;
--DROP TABLE IF EXISTS `feedback`;

CREATE TABLE IF NOT EXISTS `music` (
  `music_id` INT AUTO_INCREMENT NOT NULL,
  `path` VARCHAR(255) NOT NULL,
  `encoding` VARCHAR(255) NOT NULL,
  `track_length` INT NOT NULL,
  `file_hash` CHAR(32) NOT NULL,
  `text_tag` CHAR(36) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  `missing_at` DATETIME,
  UNIQUE KEY unique_music_1 (`path`),
  UNIQUE KEY unique_music_2 (`text_tag`),
  PRIMARY KEY (`music_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `music_tag` (
  `music_id` INT NOT NULL,
  `seq` INT NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `value` TEXT NOT NULL,
  `text_tag` CHAR(36) NOT NULL,
  UNIQUE KEY unique_music_tag_1 (`text_tag`),
  KEY key_music_tag_1 (`music_id`),
  PRIMARY KEY (`music_id`, `seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `artwork` (
  `artwork_id` INT AUTO_INCREMENT NOT NULL,
  `mimetype` VARCHAR(255) NOT NULL,
  `image` MEDIUMBLOB NOT NULL,
  `image_hash` CHAR(32) NOT NULL,
  `text_tag` CHAR(36) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  UNIQUE KEY unique_artwork_1 (`image_hash`),
  UNIQUE KEY unique_artwork_2 (`text_tag`),
  PRIMARY KEY (`artwork_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `music_artwork` (
  `music_id` INT NOT NULL,
  `artwork_id` INT NOT NULL,
  PRIMARY KEY (`music_id`, `artwork_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `text` (
  `text_id` INT AUTO_INCREMENT NOT NULL,
  `source` VARCHAR(255) NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `content` MEDIUMTEXT NOT NULL,
  `raw` MEDIUMTEXT,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  UNIQUE KEY unique_text_1 (`source`, `title`),
  PRIMARY KEY (`text_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `text_link` (
  `text_id` INT NOT NULL,
  `seq` INT NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `link_type` VARCHAR(255) NOT NULL,
  KEY key_text_link_1 (`title`),
  PRIMARY KEY (`text_id`, `seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `text_tag` (
  `text_id` INT NOT NULL,
  `tag` CHAR(36) NOT NULL,
  `relevance` INT NOT NULL,
  `created_at` DATETIME NOT NULL,
  KEY key_text_tag_1 (`tag`),
  PRIMARY KEY (`text_id`, `tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `cache` (
  `cache_id` INT AUTO_INCREMENT NOT NULL,
  `source` VARCHAR(255) NOT NULL,
  `cache_key` VARCHAR(255) NOT NULL,
  `response` MEDIUMTEXT NOT NULL,
  `request_at` DATETIME NOT NULL,
  UNIQUE KEY unique_cache_1 (`source`, `cache_key`),
  PRIMARY KEY (`cache_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `document` (
  `document_id` INT AUTO_INCREMENT NOT NULL,
  `music_id` INT NOT NULL,
  `content` MEDIUMTEXT NOT NULL,
  `content_hash` CHAR(32) NOT NULL,
  `discovery_doc_id` CHAR(36),
  `published_at` DATETIME,
  `operation_status` VARCHAR(32) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  KEY key_document_1 (`music_id`),
  KEY key_document_2 (`discovery_doc_id`),
  PRIMARY KEY (`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `request` (
  `request_tag` CHAR(36) NOT NULL,
  `text` MEDIUMTEXT NOT NULL,
  `user_tag` CHAR(36) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `playing_at` DATETIME,
  `played_at` DATETIME,
  KEY key_request_1 (`user_tag`),
  PRIMARY KEY (`request_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `track` (
  `track_tag` CHAR(36) NOT NULL,
  `request_tag` CHAR(36),
  `discovery_doc_id` CHAR(36) NOT NULL,
  `music_id` INT NOT NULL,
  `created_at` DATETIME NOT NULL,
  `played_at` DATETIME,
  KEY key_track_1 (`request_tag`),
  PRIMARY KEY (`track_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `feedback` (
  `feedback_tag` CHAR(36) NOT NULL,
  `feedback_type` VARCHAR(255) NOT NULL,
  `track_tag` CHAR(36) NOT NULL,
  `user_tag` CHAR(36) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `feedbacked_at` DATETIME,
  KEY key_feedback_1 (`track_tag`),
  KEY key_feedback_2 (`user_tag`),
  PRIMARY KEY (`feedback_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP VIEW IF EXISTS `document_source`;
CREATE VIEW `document_source` (
  `music_id`,
  `text_id`,
  `relevance`
  )
  AS
  SELECT
    `l`.`music_id`,
    `r`.`text_id`,
    MIN(`r`.`relevance`) as `relevance`
  FROM (
    SELECT
      `m`.`music_id`,
      `m`.`text_tag`
    FROM
      `music` AS `m`
    UNION SELECT
      `mt`.`music_id`,
      `mt`.`text_tag`
    FROM
      `music_tag` AS `mt`
    UNION SELECT
      `ma`.`music_id`,
      `a`.`text_tag`
    FROM
      `music_artwork` AS `ma`
    JOIN
      `artwork` AS `a`
    ON
      `ma`.`artwork_id` = `a`.artwork_id
  ) AS `l`
  JOIN (
    SELECT
      *
    FROM
      `text_tag`
  ) AS `r`
  ON
    `l`.`text_tag` = `r`.`tag`
  GROUP BY
    `l`.`music_id`,
    `r`.`text_id`
  ORDER BY
    `music_id`,
    `text_id`,
    `relevance`;

DROP VIEW IF EXISTS `training_source`;
CREATE VIEW `training_source` (
    `query`,
    `document_id`,
    `music_id`,
    `relevance`
    )
    AS
    SELECT
        `r`.`text`,
        `tf`.`discovery_doc_id`,
        `tf`.`music_id`,
        least(4, greatest(0, truncate((sum(`tf`.`relevance`) / count(*) * 4) + 0.5, 0)))
    FROM
        `request`
    AS `r`
    JOIN (
        SELECT
            `t`.`request_tag`,
            `t`.`discovery_doc_id`,
            `t`.`music_id`,
            CASE `f`.`feedback_type`
                WHEN 'LIKE' THEN 1
                WHEN 'DISLIKE' THEN -1
                END AS `relevance`
        FROM
            `feedback` AS `f`
        JOIN (
            SELECT
                `track`.*
            FROM
                `track`
            JOIN
                `music`
            ON
                `track`.`music_id` = `music`.`music_id`
                AND
                `music`.`missing_at` IS NULL
        ) AS `t`
        ON
            `f`.`track_tag` = `t`.`track_tag`
    ) AS `tf`
    ON
        `r`.`request_tag` = `tf`.`request_tag`
    GROUP BY
        `r`.`text`,
        `tf`.`music_id`,
        `tf`.`discovery_doc_id`;

