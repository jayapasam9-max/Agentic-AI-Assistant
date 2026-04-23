variable "identifier"                 { type = string }
variable "engine_version"             { type = string }
variable "instance_class"             { type = string }
variable "allocated_storage"          { type = number }
variable "vpc_id"                     { type = string }
variable "subnet_ids"                 { type = list(string) }
variable "allowed_security_group_ids" { type = list(string) }
variable "db_name"                    { type = string }
variable "username"                   { type = string }

resource "random_password" "db" {
  length  = 32
  special = true
  # RDS rejects '/', '@', '"', and ' '
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

resource "aws_secretsmanager_secret" "db_password" {
  name = "${var.identifier}/rds-password"
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id     = aws_secretsmanager_secret.db_password.id
  secret_string = random_password.db.result
}

resource "aws_db_subnet_group" "this" {
  name       = var.identifier
  subnet_ids = var.subnet_ids
}

resource "aws_security_group" "rds" {
  name   = "${var.identifier}-rds"
  vpc_id = var.vpc_id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = var.allowed_security_group_ids
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Parameter group: enable pgvector via shared_preload_libraries.
# After the DB is up, connect once and run: CREATE EXTENSION vector;
# (Flyway V1 migration does this.)
resource "aws_db_parameter_group" "pgvector" {
  name   = "${var.identifier}-pgvector"
  family = "postgres16"

  parameter {
    name         = "shared_preload_libraries"
    value        = "vector"
    apply_method = "pending-reboot"
  }
}

resource "aws_db_instance" "this" {
  identifier                  = var.identifier
  engine                      = "postgres"
  engine_version              = var.engine_version
  instance_class              = var.instance_class
  allocated_storage           = var.allocated_storage
  storage_encrypted           = true
  db_name                     = var.db_name
  username                    = var.username
  password                    = random_password.db.result
  db_subnet_group_name        = aws_db_subnet_group.this.name
  vpc_security_group_ids      = [aws_security_group.rds.id]
  parameter_group_name        = aws_db_parameter_group.pgvector.name
  backup_retention_period     = 7
  skip_final_snapshot         = true # set false in prod
  deletion_protection         = false # set true in prod
  performance_insights_enabled = true
  auto_minor_version_upgrade  = true
}

output "endpoint"           { value = aws_db_instance.this.endpoint }
output "password_secret_id" { value = aws_secretsmanager_secret.db_password.id }
