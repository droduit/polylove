-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

-- -----------------------------------------------------
-- Schema polylove
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema polylove
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `polylove` DEFAULT CHARACTER SET utf8 ;
USE `polylove` ;

-- -----------------------------------------------------
-- Table `polylove`.`users`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `polylove`.`users` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `tequila_token` VARCHAR(255) NOT NULL,
  `email` VARCHAR(85) NOT NULL,
  `pwd` VARCHAR(255) NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `gender` TINYINT(1) NOT NULL,
  `birthday` DATE NOT NULL,
  `nationality` INT NULL,
  `interests` MEDIUMTEXT NULL,
  `description` MEDIUMTEXT NULL,
  `s_gender` TINYINT(1) NOT NULL,
  `s_age_start` INT NULL,
  `s_age_end` INT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `PKNoUser_UNIQUE` (`id` ASC))
ENGINE = InnoDB
COMMENT = 'Contains all the users of the app\n';


-- -----------------------------------------------------
-- Table `polylove`.`messages`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `polylove`.`messages` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `id_recipient` INT NOT NULL,
  `id_receiver` INT NOT NULL,
  `time` DATETIME NOT NULL,
  `body` TEXT NULL,
  PRIMARY KEY (`id`),
  INDEX `RecipientToUser_idx` (`id_recipient` ASC),
  INDEX `ReceiverToUser_idx` (`id_receiver` ASC),
  CONSTRAINT `RecipientToUser`
    FOREIGN KEY (`id_recipient`)
    REFERENCES `polylove`.`users` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `ReceiverToUser`
    FOREIGN KEY (`id_receiver`)
    REFERENCES `polylove`.`users` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
COMMENT = 'Contains the messages exchanged between two users\n';


-- -----------------------------------------------------
-- Table `polylove`.`matches`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `polylove`.`matches` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `id_user1` INT NOT NULL,
  `id_user2` INT NOT NULL,
  `has_matched` TINYINT(1) NOT NULL,
  `time` DATETIME NULL,
  PRIMARY KEY (`id`),
  INDEX `User2ToUser_idx` (`id_user2` ASC),
  INDEX `User1ToUser_idx` (`id_user1` ASC),
  CONSTRAINT `User1ToUser`
    FOREIGN KEY (`id_user1`)
    REFERENCES `polylove`.`users` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `User2ToUser`
    FOREIGN KEY (`id_user2`)
    REFERENCES `polylove`.`users` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
COMMENT = 'Contains all the people\'s proposals for a user\n';


-- -----------------------------------------------------
-- Table `polylove`.`settings`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `polylove`.`settings` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `id_user` INT NOT NULL,
  `en_location` TINYINT(1) NULL,
  `free` TINYINT(1) NULL,
  PRIMARY KEY (`id`, `id_user`),
  UNIQUE INDEX `FKNoUser_UNIQUE` (`id_user` ASC),
  UNIQUE INDEX `id_UNIQUE` (`id` ASC),
  CONSTRAINT `UserToSettings`
    FOREIGN KEY (`id_user`)
    REFERENCES `polylove`.`users` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
COMMENT = 'Contains the settings of a user';


-- -----------------------------------------------------
-- Table `polylove`.`avatars`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `polylove`.`avatars` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `id_user` INT NOT NULL,
  `hairs` INT NOT NULL,
  `face` INT NOT NULL,
  `eyes` INT NOT NULL,
  `mouth` INT NOT NULL,
  `skin` INT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `id_user_UNIQUE` (`id_user` ASC),
  CONSTRAINT `userAvatar`
    FOREIGN KEY (`id_user`)
    REFERENCES `polylove`.`users` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION)
ENGINE = InnoDB
COMMENT = 'Contains the code of all components composing an avatar for a user\n';


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
