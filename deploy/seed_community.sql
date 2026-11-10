-- 筑梦家 · 装修小圈种子数据（50 用户 / 100 帖子，图片与纯文字混合）
-- 用法：mysql -uroot -p house_design < seed_community.sql

-- 可重复执行：先清理旧的种子帖子
DELETE FROM t_post WHERE user_id IN (SELECT id FROM (SELECT id FROM t_user WHERE username LIKE 'seed_user%') t);

-- 50 个种子用户（密码统一为 Seed@2026）
INSERT INTO t_user (username, email, password, nickname, avatar, created_at) VALUES
('seed_user01', 'seed01@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '林小满', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/b00e73c9-b286-4684-9148-48acdee21470.jpg', NOW() - INTERVAL 60 DAY),
('seed_user02', 'seed02@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '周予安', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/76748ecf-b5fa-49eb-bbc4-78d4b304c966.jpg', NOW() - INTERVAL 59 DAY),
('seed_user03', 'seed03@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '陈木木', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/443a2487-cff9-47ec-bc39-ec58574b0452.jpg', NOW() - INTERVAL 58 DAY),
('seed_user04', 'seed04@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '苏晴', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/17faf470-1f09-41cf-b541-f8283aa1d209.jpg', NOW() - INTERVAL 57 DAY),
('seed_user05', 'seed05@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '许一诺', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/b00e73c9-b286-4684-9148-48acdee21470.jpg', NOW() - INTERVAL 56 DAY),
('seed_user06', 'seed06@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '顾清和', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/76748ecf-b5fa-49eb-bbc4-78d4b304c966.jpg', NOW() - INTERVAL 55 DAY),
('seed_user07', 'seed07@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '唐糖', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/443a2487-cff9-47ec-bc39-ec58574b0452.jpg', NOW() - INTERVAL 54 DAY),
('seed_user08', 'seed08@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '沈知夏', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/17faf470-1f09-41cf-b541-f8283aa1d209.jpg', NOW() - INTERVAL 53 DAY),
('seed_user09', 'seed09@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '陆星河', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/b00e73c9-b286-4684-9148-48acdee21470.jpg', NOW() - INTERVAL 52 DAY),
('seed_user10', 'seed10@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '江晚晚', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/76748ecf-b5fa-49eb-bbc4-78d4b304c966.jpg', NOW() - INTERVAL 51 DAY),
('seed_user11', 'seed11@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '何遇', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/443a2487-cff9-47ec-bc39-ec58574b0452.jpg', NOW() - INTERVAL 50 DAY),
('seed_user12', 'seed12@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '温言', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/17faf470-1f09-41cf-b541-f8283aa1d209.jpg', NOW() - INTERVAL 49 DAY),
('seed_user13', 'seed13@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '白桃', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/b00e73c9-b286-4684-9148-48acdee21470.jpg', NOW() - INTERVAL 48 DAY),
('seed_user14', 'seed14@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '宋时雨', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/76748ecf-b5fa-49eb-bbc4-78d4b304c966.jpg', NOW() - INTERVAL 47 DAY),
('seed_user15', 'seed15@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '方大同', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/443a2487-cff9-47ec-bc39-ec58574b0452.jpg', NOW() - INTERVAL 46 DAY),
('seed_user16', 'seed16@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '叶知秋', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/17faf470-1f09-41cf-b541-f8283aa1d209.jpg', NOW() - INTERVAL 45 DAY),
('seed_user17', 'seed17@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '程野', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/b00e73c9-b286-4684-9148-48acdee21470.jpg', NOW() - INTERVAL 44 DAY),
('seed_user18', 'seed18@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '赵小北', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/76748ecf-b5fa-49eb-bbc4-78d4b304c966.jpg', NOW() - INTERVAL 43 DAY),
('seed_user19', 'seed19@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '吴桐', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/443a2487-cff9-47ec-bc39-ec58574b0452.jpg', NOW() - INTERVAL 42 DAY),
('seed_user20', 'seed20@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '郑一一', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/17faf470-1f09-41cf-b541-f8283aa1d209.jpg', NOW() - INTERVAL 41 DAY),
('seed_user21', 'seed21@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '韩笑笑', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/b00e73c9-b286-4684-9148-48acdee21470.jpg', NOW() - INTERVAL 40 DAY),
('seed_user22', 'seed22@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '冯嘉', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/76748ecf-b5fa-49eb-bbc4-78d4b304c966.jpg', NOW() - INTERVAL 39 DAY),
('seed_user23', 'seed23@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '邓小满', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/443a2487-cff9-47ec-bc39-ec58574b0452.jpg', NOW() - INTERVAL 38 DAY),
('seed_user24', 'seed24@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '曹屿', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/17faf470-1f09-41cf-b541-f8283aa1d209.jpg', NOW() - INTERVAL 37 DAY),
('seed_user25', 'seed25@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '彭小鱼', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/b00e73c9-b286-4684-9148-48acdee21470.jpg', NOW() - INTERVAL 36 DAY),
('seed_user26', 'seed26@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '曾阿牛', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/76748ecf-b5fa-49eb-bbc4-78d4b304c966.jpg', NOW() - INTERVAL 35 DAY),
('seed_user27', 'seed27@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '萧然', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/443a2487-cff9-47ec-bc39-ec58574b0452.jpg', NOW() - INTERVAL 34 DAY),
('seed_user28', 'seed28@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '田甜', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/17faf470-1f09-41cf-b541-f8283aa1d209.jpg', NOW() - INTERVAL 33 DAY),
('seed_user29', 'seed29@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '董咚咚', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/b00e73c9-b286-4684-9148-48acdee21470.jpg', NOW() - INTERVAL 32 DAY),
('seed_user30', 'seed30@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '袁来', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/76748ecf-b5fa-49eb-bbc4-78d4b304c966.jpg', NOW() - INTERVAL 31 DAY),
('seed_user31', 'seed31@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '潘小圆', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/443a2487-cff9-47ec-bc39-ec58574b0452.jpg', NOW() - INTERVAL 30 DAY),
('seed_user32', 'seed32@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '蒋道理', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/17faf470-1f09-41cf-b541-f8283aa1d209.jpg', NOW() - INTERVAL 29 DAY),
('seed_user33', 'seed33@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '蔡小葵', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/b00e73c9-b286-4684-9148-48acdee21470.jpg', NOW() - INTERVAL 28 DAY),
('seed_user34', 'seed34@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '余温', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/76748ecf-b5fa-49eb-bbc4-78d4b304c966.jpg', NOW() - INTERVAL 27 DAY),
('seed_user35', 'seed35@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '杜若', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/443a2487-cff9-47ec-bc39-ec58574b0452.jpg', NOW() - INTERVAL 26 DAY),
('seed_user36', 'seed36@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '戴阿福', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/17faf470-1f09-41cf-b541-f8283aa1d209.jpg', NOW() - INTERVAL 25 DAY),
('seed_user37', 'seed37@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '夏栀', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/b00e73c9-b286-4684-9148-48acdee21470.jpg', NOW() - INTERVAL 24 DAY),
('seed_user38', 'seed38@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '钟意', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/76748ecf-b5fa-49eb-bbc4-78d4b304c966.jpg', NOW() - INTERVAL 23 DAY),
('seed_user39', 'seed39@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '汪小宅', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/443a2487-cff9-47ec-bc39-ec58574b0452.jpg', NOW() - INTERVAL 22 DAY),
('seed_user40', 'seed40@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '石一', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/17faf470-1f09-41cf-b541-f8283aa1d209.jpg', NOW() - INTERVAL 21 DAY),
('seed_user41', 'seed41@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '罗小罗', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/b00e73c9-b286-4684-9148-48acdee21470.jpg', NOW() - INTERVAL 60 DAY),
('seed_user42', 'seed42@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '高饱饱', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/76748ecf-b5fa-49eb-bbc4-78d4b304c966.jpg', NOW() - INTERVAL 59 DAY),
('seed_user43', 'seed43@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '谢栖迟', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/443a2487-cff9-47ec-bc39-ec58574b0452.jpg', NOW() - INTERVAL 58 DAY),
('seed_user44', 'seed44@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '梁小鹿', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/17faf470-1f09-41cf-b541-f8283aa1d209.jpg', NOW() - INTERVAL 57 DAY),
('seed_user45', 'seed45@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '苏打', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/b00e73c9-b286-4684-9148-48acdee21470.jpg', NOW() - INTERVAL 56 DAY),
('seed_user46', 'seed46@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '卢卡斯', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/76748ecf-b5fa-49eb-bbc4-78d4b304c966.jpg', NOW() - INTERVAL 55 DAY),
('seed_user47', 'seed47@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '邱小满', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/443a2487-cff9-47ec-bc39-ec58574b0452.jpg', NOW() - INTERVAL 54 DAY),
('seed_user48', 'seed48@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '江一白', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/17faf470-1f09-41cf-b541-f8283aa1d209.jpg', NOW() - INTERVAL 53 DAY),
('seed_user49', 'seed49@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '闫青', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/b00e73c9-b286-4684-9148-48acdee21470.jpg', NOW() - INTERVAL 52 DAY),
('seed_user50', 'seed50@housedesign.local', '$2b$10$GhdqU44kv1GijQBkyFS7luGxbeBlRFo8UE4kcV1pyoso1yWGgfJnm', '梅子', 'https://mybucket0100.oss-cn-beijing.aliyuncs.com/uploads/76748ecf-b5fa-49eb-bbc4-78d4b304c966.jpg', NOW() - INTERVAL 51 DAY)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), avatar = VALUES(avatar), password = VALUES(password), email = VALUES(email);

SET @u22 = (SELECT id FROM t_user WHERE username = 'seed_user22' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u22, '新家餐厅毕业，圆桌真的更适合中国家庭，夹菜不用站起来。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img03.jpg"]', NOW() - INTERVAL 3 HOUR);
SET @u21 = (SELECT id FROM t_user WHERE username = 'seed_user21' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u21, '无主灯要提前规划回路，不然后期想加灯只能走明线。', NULL, NOW() - INTERVAL 10 HOUR);
SET @u46 = (SELECT id FROM t_user WHERE username = 'seed_user46' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u46, '客厅加了一盏落地灯，晚上不开主灯也够用。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img01.jpg"]', NOW() - INTERVAL 17 HOUR);
SET @u5 = (SELECT id FROM t_user WHERE username = 'seed_user05' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u5, '厨房插座一定要多留，尤其是水槽下面，净水器和垃圾处理器都要用。', NULL, NOW() - INTERVAL 24 HOUR);
SET @u33 = (SELECT id FROM t_user WHERE username = 'seed_user33' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u33, '收纳不是越多越好，先断舍离再定柜子尺寸。', NULL, NOW() - INTERVAL 31 HOUR);
SET @u26 = (SELECT id FROM t_user WHERE username = 'seed_user26' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u26, '第一次装修没请设计师，自己画图跑工地，最后效果居然还不错。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img01.jpg"]', NOW() - INTERVAL 38 HOUR);
SET @u1 = (SELECT id FROM t_user WHERE username = 'seed_user01' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u1, '分享一个血泪教训：踢脚线一定要和门套同色，不然再贵的装修也会有廉价感。', NULL, NOW() - INTERVAL 45 HOUR);
SET @u36 = (SELECT id FROM t_user WHERE username = 'seed_user36' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u36, '厨房换了感应灯，晚上洗菜不用摸黑。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img05.jpg"]', NOW() - INTERVAL 52 HOUR);
SET @u8 = (SELECT id FROM t_user WHERE username = 'seed_user08' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u8, '红砖墙建议做真砖薄贴，比壁纸质感好太多，就是工费贵一点。', NULL, NOW() - INTERVAL 59 HOUR);
SET @u40 = (SELECT id FROM t_user WHERE username = 'seed_user40' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u40, '客厅电视墙没做造型，留白加悬空电视柜，简单耐看。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img01.jpg"]', NOW() - INTERVAL 66 HOUR);
SET @u37 = (SELECT id FROM t_user WHERE username = 'seed_user37' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u37, '软包床头注意清洁，布艺容易积灰。', NULL, NOW() - INTERVAL 73 HOUR);
SET @u6 = (SELECT id FROM t_user WHERE username = 'seed_user06' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u6, '阳台改成了阅读角，藤椅配串灯，晚上坐这儿看城市夜景，比客厅还舒服。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img06.jpg"]', NOW() - INTERVAL 80 HOUR);
SET @u28 = (SELECT id FROM t_user WHERE username = 'seed_user28' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u28, '餐边柜记得预留插座，咖啡机、热水壶都要用。', NULL, NOW() - INTERVAL 87 HOUR);
SET @u29 = (SELECT id FROM t_user WHERE username = 'seed_user29' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u29, '厨房动线按洗切炒排，做饭效率高了一倍。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img05.jpg"]', NOW() - INTERVAL 94 HOUR);
SET @u37 = (SELECT id FROM t_user WHERE username = 'seed_user37' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u37, '奶油风卧室，床头做了软包，靠着看书很舒服。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img07.jpg"]', NOW() - INTERVAL 101 HOUR);
SET @u23 = (SELECT id FROM t_user WHERE username = 'seed_user23' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u23, '茶室不必做复杂，留白和光线比摆件更重要。', NULL, NOW() - INTERVAL 108 HOUR);
SET @u25 = (SELECT id FROM t_user WHERE username = 'seed_user25' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u25, '阳台封窗之后做了洗衣区加晾晒区，家务动线短了很多。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img06.jpg"]', NOW() - INTERVAL 115 HOUR);
SET @u47 = (SELECT id FROM t_user WHERE username = 'seed_user47' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u47, '卧室做了悬浮床，扫地机器人进出无压力。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img02.jpg"]', NOW() - INTERVAL 122 HOUR);
SET @u39 = (SELECT id FROM t_user WHERE username = 'seed_user39' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u39, '小书房用了折叠桌，不用时收起来，空间灵活很多。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img09.jpg"]', NOW() - INTERVAL 129 HOUR);
SET @u19 = (SELECT id FROM t_user WHERE username = 'seed_user19' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u19, '找木工一定要看现场样板，照片和实物差别很大。', NULL, NOW() - INTERVAL 136 HOUR);
SET @u16 = (SELECT id FROM t_user WHERE username = 'seed_user16' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u16, '主卫做了干湿分离，洗手台外移之后早上洗漱不打架。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img10.jpg"]', NOW() - INTERVAL 143 HOUR);
SET @u11 = (SELECT id FROM t_user WHERE username = 'seed_user11' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u11, '老房改造先查水电线路，我家电线还是铝线，全部换新才敢住。', NULL, NOW() - INTERVAL 150 HOUR);
SET @u17 = (SELECT id FROM t_user WHERE username = 'seed_user17' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u17, '阁楼改卧室，保留斑驳砖墙，配黑色金属床，粗粝但很安心。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img08.jpg"]', NOW() - INTERVAL 157 HOUR);
SET @u49 = (SELECT id FROM t_user WHERE username = 'seed_user49' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u49, '阳台养了两只猫，做了猫爬架和晒太阳的垫子。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img06.jpg"]', NOW() - INTERVAL 164 HOUR);
SET @u41 = (SELECT id FROM t_user WHERE username = 'seed_user41' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u41, '主卧床头背景用了木饰面，比墙纸有质感。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img02.jpg"]', NOW() - INTERVAL 171 HOUR);
SET @u25 = (SELECT id FROM t_user WHERE username = 'seed_user25' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u25, '厨房收纳做了抽屉分隔，锅具和餐具终于各归各位。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img05.jpg"]', NOW() - INTERVAL 178 HOUR);
SET @u42 = (SELECT id FROM t_user WHERE username = 'seed_user42' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u42, '阳台晒衣区加了折叠晾衣架，收起来不挡视线。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img06.jpg"]', NOW() - INTERVAL 185 HOUR);
SET @u14 = (SELECT id FROM t_user WHERE username = 'seed_user14' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u14, '租房改造第一期，阳台铺了防腐木，成本不到三百，幸福感翻倍。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img06.jpg"]', NOW() - INTERVAL 192 HOUR);
SET @u44 = (SELECT id FROM t_user WHERE username = 'seed_user44' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u44, '卫生间装了壁龛，洗发水终于不用堆地上。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img10.jpg"]', NOW() - INTERVAL 199 HOUR);
SET @u17 = (SELECT id FROM t_user WHERE username = 'seed_user17' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u17, '阁楼改造注意保温层，冬天不做好会冷得睡不着。', NULL, NOW() - INTERVAL 206 HOUR);
SET @u5 = (SELECT id FROM t_user WHERE username = 'seed_user05' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u5, '厨房做了无把手白色柜门，台面是浅色石英石，油污一擦就干净。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img05.jpg"]', NOW() - INTERVAL 213 HOUR);
SET @u24 = (SELECT id FROM t_user WHERE username = 'seed_user24' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u24, '小户型卫生间别装浴缸，淋浴房更实用。', NULL, NOW() - INTERVAL 220 HOUR);
SET @u30 = (SELECT id FROM t_user WHERE username = 'seed_user30' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u30, '洞洞板承重要看墙体，轻体墙要先加固。', NULL, NOW() - INTERVAL 227 HOUR);
SET @u32 = (SELECT id FROM t_user WHERE username = 'seed_user32' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u32, '深色床品容易显灰，选可水洗材质更省心。', NULL, NOW() - INTERVAL 234 HOUR);
SET @u38 = (SELECT id FROM t_user WHERE username = 'seed_user38' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u38, '餐厅吊灯换成了暖光，饭菜看起来都更香了。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img03.jpg"]', NOW() - INTERVAL 241 HOUR);
SET @u23 = (SELECT id FROM t_user WHERE username = 'seed_user23' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u23, '家里做了个茶室角落，蒲团加矮几，周末泡茶发呆太舒服。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img04.jpg"]', NOW() - INTERVAL 248 HOUR);
SET @u50 = (SELECT id FROM t_user WHERE username = 'seed_user50' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u50, '卫生间改造毕业照，小小三平米也能干净明亮。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img10.jpg"]', NOW() - INTERVAL 255 HOUR);
SET @u27 = (SELECT id FROM t_user WHERE username = 'seed_user27' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u27, '奶油风不是全白，加一点原木和藤编，家里才不冷。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img07.jpg"]', NOW() - INTERVAL 262 HOUR);
SET @u43 = (SELECT id FROM t_user WHERE username = 'seed_user43' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u43, '餐边柜中间留空高度建议 35 到 40cm，太矮放不下咖啡机。', NULL, NOW() - INTERVAL 269 HOUR);
SET @u7 = (SELECT id FROM t_user WHERE username = 'seed_user07' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u7, '奶油风客厅毕业照，弧形门洞和云朵沙发是全场最爱，朋友来了都不想走。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img07.jpg"]', NOW() - INTERVAL 276 HOUR);
SET @u19 = (SELECT id FROM t_user WHERE username = 'seed_user19' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u19, '拱门加奶油墙，整个家都软下来了。木工师傅手艺是关键。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img07.jpg"]', NOW() - INTERVAL 283 HOUR);
SET @u12 = (SELECT id FROM t_user WHERE username = 'seed_user12' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u12, '厨房台面做了高低台，洗菜不弯腰，做饭肩膀轻松很多。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img05.jpg"]', NOW() - INTERVAL 290 HOUR);
SET @u20 = (SELECT id FROM t_user WHERE username = 'seed_user20' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u20, '儿童房家具尽量选圆角，柜子固定上墙，安全永远排第一。', NULL, NOW() - INTERVAL 297 HOUR);
SET @u21 = (SELECT id FROM t_user WHERE username = 'seed_user21' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u21, '客厅灯光改成无主灯，筒灯加灯带，晚上氛围感直接拉满。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img01.jpg"]', NOW() - INTERVAL 304 HOUR);
SET @u10 = (SELECT id FROM t_user WHERE username = 'seed_user10' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u10, '卫生间改造完成，水磨石地面配小白砖，黄铜五金点睛，洗澡都像在做 SPA。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img10.jpg"]', NOW() - INTERVAL 311 HOUR);
SET @u34 = (SELECT id FROM t_user WHERE username = 'seed_user34' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u34, '卧室窗帘做了双层，纱帘加遮光帘，周末睡到自然醒。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img02.jpg"]', NOW() - INTERVAL 318 HOUR);
SET @u31 = (SELECT id FROM t_user WHERE username = 'seed_user31' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u31, '把阳台改成休闲区，弧形沙发配小圆几，周末不想出门。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img07.jpg"]', NOW() - INTERVAL 325 HOUR);
SET @u31 = (SELECT id FROM t_user WHERE username = 'seed_user31' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u31, '客厅铺了整块大地毯，冬天光脚踩上去也不冷。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img01.jpg"]', NOW() - INTERVAL 332 HOUR);
SET @u4 = (SELECT id FROM t_user WHERE username = 'seed_user04' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u4, '买家具前先量电梯和门洞尺寸，我那张沙发卡在楼道半小时，太尴尬了。', NULL, NOW() - INTERVAL 339 HOUR);
SET @u18 = (SELECT id FROM t_user WHERE username = 'seed_user18' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u18, '客厅去掉了茶几，换成大地毯加边几，孩子在地上玩也不怕磕碰。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img01.jpg"]', NOW() - INTERVAL 346 HOUR);
SET @u50 = (SELECT id FROM t_user WHERE username = 'seed_user50' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u50, '奶油风小客厅，两个人住刚刚好，温馨不拥挤。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img07.jpg"]', NOW() - INTERVAL 353 HOUR);
SET @u24 = (SELECT id FROM t_user WHERE username = 'seed_user24' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u24, '小卫生间扩容思路：壁挂洗手台加窄边镜柜，省出一整块活动空间。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img10.jpg"]', NOW() - INTERVAL 360 HOUR);
SET @u2 = (SELECT id FROM t_user WHERE username = 'seed_user02' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u2, '次卧改成了北欧风，灰墙配白床品真的怎么拍都好看。床垫选了偏硬的，腰不好的人慎选软床。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img02.jpg"]', NOW() - INTERVAL 367 HOUR);
SET @u26 = (SELECT id FROM t_user WHERE username = 'seed_user26' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u26, '装修合同一定要写清增项规则，不然结算时容易超预算。', NULL, NOW() - INTERVAL 374 HOUR);
SET @u9 = (SELECT id FROM t_user WHERE username = 'seed_user09' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u9, '书房毕业，书桌靠窗，光线刚刚好。人体工学椅真的值得投资。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img09.jpg"]', NOW() - INTERVAL 381 HOUR);
SET @u20 = (SELECT id FROM t_user WHERE username = 'seed_user20' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u20, '儿童房用了可擦洗墙漆，床头留了整面涂鸦墙，孩子画得可开心了。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img02.jpg"]', NOW() - INTERVAL 388 HOUR);
SET @u30 = (SELECT id FROM t_user WHERE username = 'seed_user30' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u30, '书桌后面做了洞洞板，工具和耳机都上墙，桌面清爽多了。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img09.jpg"]', NOW() - INTERVAL 395 HOUR);
SET @u35 = (SELECT id FROM t_user WHERE username = 'seed_user35' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u35, '阳台做了小花园，薄荷和迷迭香都种上了，做饭随手摘。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img06.jpg"]', NOW() - INTERVAL 402 HOUR);
SET @u12 = (SELECT id FROM t_user WHERE username = 'seed_user12' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u12, '主卧选了低饱和的灰绿色墙，配白色床品很安静，睡前看手机都少了。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img02.jpg"]', NOW() - INTERVAL 409 HOUR);
SET @u32 = (SELECT id FROM t_user WHERE username = 'seed_user32' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u32, '卧室保留工业风，床品用了深灰色，耐脏又高级。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img08.jpg"]', NOW() - INTERVAL 416 HOUR);
SET @u13 = (SELECT id FROM t_user WHERE username = 'seed_user13' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u13, '餐厅做了卡座，靠墙一排收纳柜，小户型真的可以抄作业。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img03.jpg"]', NOW() - INTERVAL 423 HOUR);
SET @u3 = (SELECT id FROM t_user WHERE username = 'seed_user03' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u3, '书房做了整墙书柜，收纳和氛围感一次到位。提醒：书架进深 30cm 就够了，太深反而浪费空间。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img09.jpg"]', NOW() - INTERVAL 430 HOUR);
SET @u4 = (SELECT id FROM t_user WHERE username = 'seed_user04' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u4, '日式侘寂风客厅，微水泥墙面配矮桌，住进来之后整个人都慢下来了。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img04.jpg"]', NOW() - INTERVAL 437 HOUR);
SET @u34 = (SELECT id FROM t_user WHERE username = 'seed_user34' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u34, '书房和卧室之间做了玻璃隔断，采光和隐私兼顾。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img09.jpg"]', NOW() - INTERVAL 444 HOUR);
SET @u42 = (SELECT id FROM t_user WHERE username = 'seed_user42' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u42, '厨房做了转角拉篮，调料瓶终于不堆在台面上了。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img05.jpg"]', NOW() - INTERVAL 451 HOUR);
SET @u10 = (SELECT id FROM t_user WHERE username = 'seed_user10' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u10, '卫生间防水一定要做闭水试验，48 小时别偷懒，返工代价太大。', NULL, NOW() - INTERVAL 458 HOUR);
SET @u40 = (SELECT id FROM t_user WHERE username = 'seed_user40' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u40, '次卧改成工业风，朋友来了都说像咖啡店。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img08.jpg"]', NOW() - INTERVAL 465 HOUR);
SET @u22 = (SELECT id FROM t_user WHERE username = 'seed_user22' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u22, '给老公做了独立书房，整墙书柜加升降桌，他说不想出书房了。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img09.jpg"]', NOW() - INTERVAL 472 HOUR);
SET @u46 = (SELECT id FROM t_user WHERE username = 'seed_user46' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u46, '工业风客厅用了水泥漆，配皮质沙发，质感很硬朗。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img08.jpg"]', NOW() - INTERVAL 479 HOUR);
SET @u11 = (SELECT id FROM t_user WHERE username = 'seed_user11' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u11, '老房改造第一套，原来客厅又暗又挤，砸掉隔墙之后通透多了。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img01.jpg"]', NOW() - INTERVAL 486 HOUR);
SET @u1 = (SELECT id FROM t_user WHERE username = 'seed_user01' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u1, '客厅终于毕业啦！从硬装到软装折腾了三个月，奶油白墙面配原木地板，落地窗采光太治愈了。预算控在 18 万，比预想省了两万。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img01.jpg", "https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img07.jpg"]', NOW() - INTERVAL 493 HOUR);
SET @u48 = (SELECT id FROM t_user WHERE username = 'seed_user48' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u48, '厨房台面选了哑光岩板，指纹不明显，打理轻松。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img05.jpg"]', NOW() - INTERVAL 500 HOUR);
SET @u29 = (SELECT id FROM t_user WHERE username = 'seed_user29' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u29, '卫生间镜柜里留了插座，吹风机和电动牙刷充电不用抢。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img10.jpg"]', NOW() - INTERVAL 507 HOUR);
SET @u47 = (SELECT id FROM t_user WHERE username = 'seed_user47' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u47, '悬浮床要确认承重和安装方式，别只看颜值。', NULL, NOW() - INTERVAL 514 HOUR);
SET @u27 = (SELECT id FROM t_user WHERE username = 'seed_user27' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u27, '卧室只留了床头灯和壁灯，睡前光线柔和不刺眼。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img02.jpg"]', NOW() - INTERVAL 521 HOUR);
SET @u45 = (SELECT id FROM t_user WHERE username = 'seed_user45' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u45, '木地板要留伸缩缝，门口位置记得做收边条。', NULL, NOW() - INTERVAL 528 HOUR);
SET @u13 = (SELECT id FROM t_user WHERE username = 'seed_user13' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u13, '定制柜一定要留检修口，燃气表和阀门被柜子封死会很麻烦。', NULL, NOW() - INTERVAL 535 HOUR);
SET @u36 = (SELECT id FROM t_user WHERE username = 'seed_user36' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u36, '客厅沙发选了可拆洗面料，有猫家庭慎选丝绒。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img01.jpg"]', NOW() - INTERVAL 542 HOUR);
SET @u43 = (SELECT id FROM t_user WHERE username = 'seed_user43' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u43, '餐厅做了满墙餐边柜，中间留空放咖啡机，实用又好看。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img03.jpg"]', NOW() - INTERVAL 549 HOUR);
SET @u39 = (SELECT id FROM t_user WHERE username = 'seed_user39' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u39, '折叠家具五金件要选好的，便宜的五金用半年就松。', NULL, NOW() - INTERVAL 556 HOUR);
SET @u33 = (SELECT id FROM t_user WHERE username = 'seed_user33' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u33, '日式收纳做了一半，抽屉和隔板都按物品尺寸定制，太治愈了。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img04.jpg"]', NOW() - INTERVAL 563 HOUR);
SET @u15 = (SELECT id FROM t_user WHERE username = 'seed_user15' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u15, '把次卧改成了书房加衣帽间，一面墙两用，空间利用率直接拉满。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img09.jpg"]', NOW() - INTERVAL 570 HOUR);
SET @u14 = (SELECT id FROM t_user WHERE username = 'seed_user14' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u14, '租房改造优先做可带走的软装，地毯、灯具、挂画，搬家还能继续用。', NULL, NOW() - INTERVAL 577 HOUR);
SET @u45 = (SELECT id FROM t_user WHERE username = 'seed_user45' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u45, '全屋通铺木地板，视觉延伸感很好，小户型显大。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img07.jpg"]', NOW() - INTERVAL 584 HOUR);
SET @u49 = (SELECT id FROM t_user WHERE username = 'seed_user49' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u49, '养猫家庭沙发底下要留扫地机器人高度。', NULL, NOW() - INTERVAL 591 HOUR);
SET @u3 = (SELECT id FROM t_user WHERE username = 'seed_user03' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u3, '新中式餐厅落地，圆桌配藤编椅，长辈来了都夸有家的味道。木作是本地工厂定制的，性价比高。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img03.jpg"]', NOW() - INTERVAL 598 HOUR);
SET @u28 = (SELECT id FROM t_user WHERE username = 'seed_user28' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u28, '餐边柜做了玻璃门，杯子和酒都看得见，拿取方便还好看。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img03.jpg"]', NOW() - INTERVAL 605 HOUR);
SET @u38 = (SELECT id FROM t_user WHERE username = 'seed_user38' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u38, '卫生间加了电热毛巾架，梅雨季再也不怕毛巾发潮。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img10.jpg"]', NOW() - INTERVAL 612 HOUR);
SET @u6 = (SELECT id FROM t_user WHERE username = 'seed_user06' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u6, '客厅没做复杂吊顶，只走了一圈石膏线，层高显高不少。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img01.jpg"]', NOW() - INTERVAL 619 HOUR);
SET @u35 = (SELECT id FROM t_user WHERE username = 'seed_user35' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u35, '阳台种菜注意排水，托盘积水容易招虫。', NULL, NOW() - INTERVAL 626 HOUR);
SET @u44 = (SELECT id FROM t_user WHERE username = 'seed_user44' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u44, '侘寂风客厅用了亚麻沙发，越坐越有味道。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img04.jpg"]', NOW() - INTERVAL 633 HOUR);
SET @u7 = (SELECT id FROM t_user WHERE username = 'seed_user07' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u7, '奶油风最容易翻车的是色差，墙漆、柜门、沙发一定要拿同一块色卡对比。', NULL, NOW() - INTERVAL 640 HOUR);
SET @u9 = (SELECT id FROM t_user WHERE username = 'seed_user09' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u9, '家里留了一个榻榻米角落，喝茶看书都合适，朋友来了还能打地铺。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img04.jpg"]', NOW() - INTERVAL 647 HOUR);
SET @u15 = (SELECT id FROM t_user WHERE username = 'seed_user15' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u15, '开放式厨房真香，但油烟机一定要选大吸力的，不然客厅全是味。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img05.jpg"]', NOW() - INTERVAL 654 HOUR);
SET @u16 = (SELECT id FROM t_user WHERE username = 'seed_user16' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u16, '淋浴房玻璃建议贴防爆膜，安全第一。', NULL, NOW() - INTERVAL 661 HOUR);
SET @u18 = (SELECT id FROM t_user WHERE username = 'seed_user18' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u18, '阳台种了一排绿植，配上暖光灯，晚上回家心情都会变好。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img06.jpg"]', NOW() - INTERVAL 668 HOUR);
SET @u48 = (SELECT id FROM t_user WHERE username = 'seed_user48' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u48, '书房整墙洞洞板加书桌，工具收纳一目了然。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img09.jpg"]', NOW() - INTERVAL 675 HOUR);
SET @u2 = (SELECT id FROM t_user WHERE username = 'seed_user02' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u2, '窗帘建议做满墙，视觉上房间能大一圈，亲测有效。', NULL, NOW() - INTERVAL 682 HOUR);
SET @u8 = (SELECT id FROM t_user WHERE username = 'seed_user08' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u8, '工业风卧室，红砖墙配黑铁床，暖色灯泡氛围感拉满。', '["https://mybucket0100.oss-cn-beijing.aliyuncs.com/seed/community/img08.jpg"]', NOW() - INTERVAL 689 HOUR);
SET @u41 = (SELECT id FROM t_user WHERE username = 'seed_user41' LIMIT 1);
INSERT INTO t_post (user_id, content, images, created_at) VALUES (@u41, '木饰面要留伸缩缝，不然季节变化容易起拱。', NULL, NOW() - INTERVAL 696 HOUR);
