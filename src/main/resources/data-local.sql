-- 테스트용 사용자 데이터
INSERT INTO users (email, nickname, login_type, friend_code, status, created_at) VALUES ('test@example.com', '테스트유저', 'EMAIL', 'TEST1234', 'ACTIVE', NOW());

-- 테스트용 포즈 데이터
INSERT INTO poses (title, image_url, people_count, created_at) VALUES ('커플 포즈', 'https://example.com/pose1.jpg', 2, NOW());
INSERT INTO poses (title, image_url, people_count, created_at) VALUES ('솔로 포즈', 'https://example.com/pose2.jpg', 1, NOW());
INSERT INTO poses (title, image_url, people_count, created_at) VALUES ('단체 포즈', 'https://example.com/pose3.jpg', 4, NOW());
