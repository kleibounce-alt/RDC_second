CREATE TABLE `sys_user` (
                            `id` BIGINT NOT NULL AUTO_INCREMENT,
                            `username` VARCHAR(50) NOT NULL,
                            `password` VARCHAR(255) NOT NULL,
                            `nickname` VARCHAR(50) DEFAULT NULL,
                            `avatar` VARCHAR(255) DEFAULT NULL,
                            `email` VARCHAR(100) DEFAULT NULL,
                            `phone` VARCHAR(20) DEFAULT NULL,
                            `vip_level` INT DEFAULT '0',
                            `vip_expire_time` DATETIME DEFAULT NULL,
                            `ban_end_time` DATETIME DEFAULT NULL,
                            `status` ENUM('NORMAL','BANNED') DEFAULT 'NORMAL',
                            `is_deleted` TINYINT(1) DEFAULT '0',
                            `created_at` DATETIME,
                            `updated_at` DATETIME,
                            PRIMARY KEY (`id`),
                            UNIQUE (`username`)
);

CREATE TABLE `sys_role` (
                            `id` BIGINT NOT NULL AUTO_INCREMENT,
                            `name` VARCHAR(50) NOT NULL,
                            `code` VARCHAR(50) NOT NULL,
                            `is_deleted` TINYINT(1) DEFAULT '0',
                            `created_at` DATETIME,
                            PRIMARY KEY (`id`),
                            UNIQUE (`code`)
);

CREATE TABLE `sys_permission` (
                                  `id` BIGINT NOT NULL AUTO_INCREMENT,
                                  `name` VARCHAR(50) NOT NULL,
                                  `code` VARCHAR(50) NOT NULL,
                                  `url_pattern` VARCHAR(255) DEFAULT NULL,
                                  `is_deleted` TINYINT(1) DEFAULT '0',
                                  `created_at` DATETIME,
                                  PRIMARY KEY (`id`)
);

CREATE TABLE `sys_user_role` (
                                 `user_id` BIGINT NOT NULL,
                                 `role_id` BIGINT NOT NULL,
                                 `is_deleted` TINYINT(1) DEFAULT '0',
                                 `created_at` DATETIME,
                                 PRIMARY KEY (`user_id`,`role_id`)
);

CREATE TABLE `sys_role_permission` (
                                       `role_id` BIGINT NOT NULL,
                                       `permission_id` BIGINT NOT NULL,
                                       `is_deleted` TINYINT(1) DEFAULT '0',
                                       `created_at` DATETIME,
                                       PRIMARY KEY (`role_id`,`permission_id`)
);

--------------------------------------------------------------------------
--------------------------------------------------------------------------

CREATE TABLE `product` (
                           `id` BIGINT NOT NULL AUTO_INCREMENT,
                           `user_id` BIGINT NOT NULL,
                           `title` VARCHAR(200) NOT NULL,
                           `description` TEXT,
                           `price` DECIMAL(10,2) NOT NULL,
                           `stock` INT DEFAULT '1',
                           `status` ENUM('PENDING','AUDITING','PUBLISHED','REJECTED','SOLD') DEFAULT 'PENDING',
                           `version` INT DEFAULT '0',
                           `view_count` INT DEFAULT '0',
                           `exposure_weight` INT DEFAULT '0',
                           `reject_reason` VARCHAR(255) DEFAULT NULL,
                           `is_deleted` TINYINT(1) DEFAULT '0',
                           `created_at` DATETIME,
                           `updated_at` DATETIME,
                           PRIMARY KEY (`id`)
);

CREATE TABLE `product_image` (
                                 `id` BIGINT NOT NULL AUTO_INCREMENT,
                                 `product_id` BIGINT NOT NULL,
                                 `image_url` VARCHAR(255) NOT NULL,
                                 `sort` INT DEFAULT '0',
                                 `is_deleted` TINYINT(1) DEFAULT '0',
                                 `created_at` DATETIME,
                                 PRIMARY KEY (`id`)
);

CREATE TABLE `tag` (
                       `id` BIGINT NOT NULL AUTO_INCREMENT,
                       `name` VARCHAR(50) NOT NULL,
                       `is_deleted` TINYINT(1) DEFAULT '0',
                       `created_at` DATETIME,
                       PRIMARY KEY (`id`),
                       UNIQUE (`name`)
);

CREATE TABLE `product_tag` (
                               `product_id` BIGINT NOT NULL,
                               `tag_id` BIGINT NOT NULL,
                               `is_deleted` TINYINT(1) DEFAULT '0',
                               `created_at` DATETIME,
                               PRIMARY KEY (`product_id`,`tag_id`)
);

CREATE TABLE `favorite` (
                            `user_id` BIGINT NOT NULL,
                            `product_id` BIGINT NOT NULL,
                            `is_deleted` TINYINT(1) DEFAULT '0',
                            `created_at` DATETIME,
                            `updated_at` DATETIME,
                            PRIMARY KEY (`user_id`,`product_id`)
);

CREATE TABLE `follow` (
                          `user_id` BIGINT NOT NULL,
                          `follow_user_id` BIGINT NOT NULL,
                          `is_deleted` TINYINT(1) DEFAULT '0',
                          `created_at` DATETIME,
                          `updated_at` DATETIME,
                          PRIMARY KEY (`user_id`,`follow_user_id`)
);

CREATE TABLE `comment` (
                           `id` BIGINT NOT NULL AUTO_INCREMENT,
                           `product_id` BIGINT NOT NULL,
                           `user_id` BIGINT NOT NULL,
                           `content` TEXT NOT NULL,
                           `like_count` INT DEFAULT '0',
                           `is_deleted` TINYINT(1) DEFAULT '0',
                           `created_at` DATETIME,
                           PRIMARY KEY (`id`)
);

CREATE TABLE `comment_likes` (
                                 `user_id` BIGINT NOT NULL,
                                 `comment_id` BIGINT NOT NULL,
                                 `is_deleted` TINYINT(1) DEFAULT '0',
                                 `created_at` DATETIME,
                                 PRIMARY KEY (`user_id`,`comment_id`)
);

CREATE TABLE `wallet` (
                          `id` BIGINT NOT NULL AUTO_INCREMENT,
                          `user_id` BIGINT NOT NULL,
                          `balance` DECIMAL(10,2) DEFAULT '0.00',
                          `is_deleted` TINYINT(1) DEFAULT '0',
                          `created_at` DATETIME,
                          `updated_at` DATETIME,
                          PRIMARY KEY (`id`),
                          UNIQUE (`user_id`)
);

CREATE TABLE `wallet_record` (
                                 `id` BIGINT NOT NULL AUTO_INCREMENT,
                                 `user_id` BIGINT NOT NULL,
                                 `type` ENUM('RECHARGE','PAYMENT','REFUND','VIP') NOT NULL,
                                 `amount` DECIMAL(10,2) NOT NULL,
                                 `balance_after` DECIMAL(10,2) NOT NULL,
                                 `remark` VARCHAR(255) DEFAULT NULL,
                                 `is_deleted` TINYINT(1) DEFAULT '0',
                                 `created_at` DATETIME,
                                 PRIMARY KEY (`id`)
);

CREATE TABLE `orders` (
                          `id` BIGINT NOT NULL AUTO_INCREMENT,
                          `order_no` VARCHAR(64) NOT NULL,
                          `buyer_id` BIGINT NOT NULL,
                          `seller_id` BIGINT NOT NULL,
                          `product_id` BIGINT NOT NULL,
                          `price` DECIMAL(10,2) NOT NULL,
                          `status` ENUM('PENDING','PAID','CANCELLED','COMPLETED') DEFAULT 'PENDING',
                          `is_deleted` TINYINT(1) DEFAULT '0',
                          `created_at` DATETIME,
                          `updated_at` DATETIME,
                          PRIMARY KEY (`id`),
                          UNIQUE (`order_no`)
);

CREATE TABLE `message` (
                           `id` BIGINT NOT NULL AUTO_INCREMENT,
                           `user_id` BIGINT NOT NULL,
                           `type` ENUM('PRODUCT_SOLD','FOLLOW_NEW_PRODUCT','AUDIT_RESULT','SYSTEM') NOT NULL,
                           `content` TEXT NOT NULL,
                           `is_read` TINYINT(1) DEFAULT '0',
                           `is_deleted` TINYINT(1) DEFAULT '0',
                           `created_at` DATETIME,
                           PRIMARY KEY (`id`)
);

CREATE TABLE `chat_record` (
                               `id` BIGINT NOT NULL AUTO_INCREMENT,
                               `sender_id` BIGINT NOT NULL,
                               `receiver_id` BIGINT NOT NULL,
                               `product_id` BIGINT DEFAULT NULL,
                               `content` TEXT NOT NULL,
                               `is_deleted` TINYINT(1) DEFAULT '0',
                               `created_at` DATETIME,
                               PRIMARY KEY (`id`)
);

CREATE TABLE `audit_log` (
                             `id` BIGINT NOT NULL AUTO_INCREMENT,
                             `product_id` BIGINT NOT NULL,
                             `admin_id` BIGINT NOT NULL,
                             `action` ENUM('APPROVE','REJECT') NOT NULL,
                             `reason` VARCHAR(255) DEFAULT NULL,
                             `is_deleted` TINYINT(1) DEFAULT '0',
                             `created_at` DATETIME,
                             PRIMARY KEY (`id`)
);

CREATE TABLE `vip_order` (
                             `id` BIGINT NOT NULL AUTO_INCREMENT,
                             `user_id` BIGINT NOT NULL,
                             `vip_level` INT NOT NULL,
                             `duration_months` INT NOT NULL,
                             `price` DECIMAL(10,2) NOT NULL,
                             `status` ENUM('PENDING','PAID','CANCELLED') DEFAULT 'PENDING',
                             `is_deleted` TINYINT(1) DEFAULT '0',
                             `created_at` DATETIME,
                             `updated_at` DATETIME,
                             PRIMARY KEY (`id`)
);

CREATE TABLE `sensitive_word`(
                            `id` BIGINT NOT NULL AUTO_INCREMENT,
                            `word` VARCHAR(50) NOT NULL,
                            `is_deleted` TINYINT(1) DEFAULT '0',
                            `created_at` DATETIME,
                            PRIMARY KEY (`id`),
                            UNIQUE (`word`)
);