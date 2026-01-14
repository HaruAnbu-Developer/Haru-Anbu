DROP DATABASE IF EXISTS cheongchun_dev;

CREATE DATABASE cheongchun_dev;

DROP USER IF EXISTS 'devuser' @ '%';

CREATE USER 'devuser' @ '%' IDENTIFIED BY 'devpass';

GRANT ALL PRIVILEGES ON cheongchun_dev.* TO 'devuser' @ '%';

FLUSH PRIVILEGES;