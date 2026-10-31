-- 演示用分类与模板。封面由 scripts/generate-demo-covers.mjs 生成，放在 classpath:/demo/covers/。
-- 模板覆盖 1/2/3 张素材、不同格式和大小上限，用于演示分类切换和上传校验。

INSERT INTO category (id, name, sort_order, created_at) VALUES
    ('creative', '创意图片', 1, UTC_TIMESTAMP(3)),
    ('festival', '节日祝福', 2, UTC_TIMESTAMP(3)),
    ('portrait', '人像写真', 3, UTC_TIMESTAMP(3));

INSERT INTO template (id, category_id, title, cover_url, description, required_image_count,
                      accepted_mime_types, max_file_size_bytes, sort_order, created_at) VALUES
    ('demo-postcard', 'creative', '旅行明信片', '/api/files/demo/covers/postcard.svg',
     '上传两张旅行照片，模拟生成一张正反面明信片。', 2,
     '["image/jpeg", "image/png"]', 5242880, 1, UTC_TIMESTAMP(3)),
    ('demo-polaroid', 'creative', '拍立得照片墙', '/api/files/demo/covers/polaroid.svg',
     '上传三张日常照片，模拟拼成一面拍立得照片墙。', 3,
     '["image/jpeg", "image/png", "image/webp"]', 5242880, 2, UTC_TIMESTAMP(3)),
    ('demo-retro-poster', 'creative', '复古海报', '/api/files/demo/covers/retro-poster.svg',
     '上传一张照片，模拟生成一张复古风格海报。', 1,
     '["image/jpeg", "image/png"]', 10485760, 3, UTC_TIMESTAMP(3)),
    ('demo-spring-festival', 'festival', '新春贺卡', '/api/files/demo/covers/spring-festival.svg',
     '上传一张全家福，模拟生成新春祝福贺卡。', 1,
     '["image/jpeg", "image/png"]', 5242880, 1, UTC_TIMESTAMP(3)),
    ('demo-birthday', 'festival', '生日祝福卡', '/api/files/demo/covers/birthday.svg',
     '上传两张照片，模拟生成一张生日祝福卡。', 2,
     '["image/jpeg", "image/png", "image/webp"]', 5242880, 2, UTC_TIMESTAMP(3)),
    ('demo-christmas', 'festival', '圣诞合影', '/api/files/demo/covers/christmas.svg',
     '上传一张合影，模拟加上圣诞场景装饰。', 1,
     '["image/jpeg", "image/png"]', 5242880, 3, UTC_TIMESTAMP(3)),
    ('demo-id-photo', 'portrait', '证件照换底', '/api/files/demo/covers/id-photo.svg',
     '上传一张正面半身照，模拟生成蓝底证件照。仅支持 JPG，不超过 2MB。', 1,
     '["image/jpeg"]', 2097152, 1, UTC_TIMESTAMP(3)),
    ('demo-couple', 'portrait', '情侣写真', '/api/files/demo/covers/couple.svg',
     '上传两人各一张照片，模拟合成一张情侣写真。', 2,
     '["image/jpeg", "image/png"]', 5242880, 2, UTC_TIMESTAMP(3));
