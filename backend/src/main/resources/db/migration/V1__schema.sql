-- 表结构说明见 docs/design.md 第 4 节。时间统一存 UTC。

CREATE TABLE app_user (
    id         VARCHAR(36) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE category (
    id         VARCHAR(64)  NOT NULL,
    name       VARCHAR(64)  NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    created_at DATETIME(3)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE template (
    id                   VARCHAR(64)   NOT NULL,
    category_id          VARCHAR(64)   NOT NULL,
    title                VARCHAR(128)  NOT NULL,
    cover_url            VARCHAR(512)  NOT NULL,
    description          VARCHAR(1024) NOT NULL,
    required_image_count INT           NOT NULL,
    accepted_mime_types  JSON          NOT NULL,
    max_file_size_bytes  BIGINT        NOT NULL,
    sort_order           INT           NOT NULL DEFAULT 0,
    created_at           DATETIME(3)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_template_category (category_id, sort_order),
    CONSTRAINT fk_template_category FOREIGN KEY (category_id) REFERENCES category (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE asset (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       VARCHAR(36)  NOT NULL,
    template_id   VARCHAR(64)  NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    mime_type     VARCHAR(64)  NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    storage_path  VARCHAR(512) NOT NULL,
    created_at    DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_asset_user (user_id),
    CONSTRAINT fk_asset_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_asset_template FOREIGN KEY (template_id) REFERENCES template (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE task (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    user_id      VARCHAR(36)   NOT NULL,
    template_id  VARCHAR(64)   NOT NULL,
    status       VARCHAR(16)   NOT NULL,
    stage        VARCHAR(32)   NULL,
    result_url   VARCHAR(512)  NULL,
    fail_reason  VARCHAR(512)  NULL,
    sim_config   JSON          NOT NULL,
    created_at   DATETIME(3)   NOT NULL,
    started_at   DATETIME(3)   NULL,
    finished_at  DATETIME(3)   NULL,
    updated_at   DATETIME(3)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_task_user_created (user_id, created_at),
    KEY idx_task_status (status),
    CONSTRAINT fk_task_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_task_template FOREIGN KEY (template_id) REFERENCES template (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE task_asset (
    task_id    BIGINT NOT NULL,
    asset_id   BIGINT NOT NULL,
    slot_index INT    NOT NULL,
    PRIMARY KEY (task_id, slot_index),
    KEY idx_task_asset_asset (asset_id),
    CONSTRAINT fk_task_asset_task FOREIGN KEY (task_id) REFERENCES task (id),
    CONSTRAINT fk_task_asset_asset FOREIGN KEY (asset_id) REFERENCES asset (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE task_step (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    task_id     BIGINT       NOT NULL,
    stage       VARCHAR(32)  NOT NULL,
    status      VARCHAR(16)  NOT NULL,
    message     VARCHAR(512) NULL,
    started_at  DATETIME(3)  NOT NULL,
    finished_at DATETIME(3)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_step_task_stage (task_id, stage),
    CONSTRAINT fk_task_step_task FOREIGN KEY (task_id) REFERENCES task (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
