DROP USER IF EXISTS 'jobportal'@'%';

CREATE USER 'jobportal'@'localhost' IDENTIFIED BY 'jobportal';

GRANT ALL PRIVILEGES ON * . * TO 'jobportal'@'localhost';