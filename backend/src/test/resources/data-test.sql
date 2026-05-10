INSERT INTO category (id, name, code, description, sort_order, status) VALUES
    (1, 'Technology', 'TECH', 'Technology news', 1, 1),
    (2, 'Learning', 'LEARNING', 'Learning news', 2, 1);

INSERT INTO tag (id, category_id, name, code, sort_order, status) VALUES
    (1, 1, 'AI', 'AI', 1, 1),
    (2, 1, 'Product', 'PRODUCT', 2, 1),
    (3, 2, 'Study', 'STUDY', 3, 1);

INSERT INTO user (id, username, password, nickname, email, role, status) VALUES
    (1, 'admin', '$2a$10$N767DsiTB9pxNDoOvHbCROgZQ/Jp45JuoysAZQ7HM.ublqiG7PNju', 'Admin', 'admin@test.local', 'ADMIN', 1),
    (2, 'reader', '$2a$10$N767DsiTB9pxNDoOvHbCROgZQ/Jp45JuoysAZQ7HM.ublqiG7PNju', 'Reader', 'reader@test.local', 'USER', 1);

INSERT INTO news (
    id, title, summary, content, source_name, source_url, cover_image,
    category_id, publish_time, crawl_time, status, view_count, like_count, favorite_count, comment_count, heat_score
) VALUES
    (1, 'AI campus tools are becoming mainstream', 'AI assistants are now part of study workflows.', 'Detail content 1', 'Tech Weekly', 'https://test.local/news/1', 'https://test.local/img/1.jpg', 1, TIMESTAMP '2026-05-06 09:20:00', TIMESTAMP '2026-05-06 09:35:00', 1, 186, 38, 26, 4, 88.5),
    (2, 'Open-source LLM delivery is accelerating', 'Teams focus on practical deployment.', 'Detail content 2', 'Model Daily', 'https://test.local/news/2', 'https://test.local/img/2.jpg', 1, TIMESTAMP '2026-05-05 14:10:00', TIMESTAMP '2026-05-05 14:25:00', 1, 152, 29, 18, 3, 81.3),
    (3, 'Small teams use AI to validate products', 'AI lowers experiment cost for indie builders.', 'Detail content 3', 'Product Journal', 'https://test.local/news/3', 'https://test.local/img/3.jpg', 1, TIMESTAMP '2026-05-04 11:40:00', TIMESTAMP '2026-05-04 12:00:00', 1, 134, 24, 16, 2, 77.9),
    (4, 'A better note-taking loop for students', 'Structured notes improve long-term retention.', 'Detail content 4', 'Learning Lab', 'https://test.local/news/4', 'https://test.local/img/4.jpg', 2, TIMESTAMP '2026-05-04 18:30:00', TIMESTAMP '2026-05-04 18:45:00', 1, 122, 19, 12, 1, 69.7);

INSERT INTO news_tag (id, news_id, tag_id) VALUES
    (1, 1, 1),
    (2, 1, 2),
    (3, 2, 1),
    (4, 3, 1),
    (5, 3, 2),
    (6, 4, 3);
