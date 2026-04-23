variable "cluster_name"               { type = string }
variable "kafka_version"              { type = string }
variable "broker_count"               { type = number }
variable "instance_type"              { type = string }
variable "vpc_id"                     { type = string }
variable "subnet_ids"                 { type = list(string) }
variable "allowed_security_group_ids" { type = list(string) }

resource "aws_security_group" "msk" {
  name   = "${var.cluster_name}-msk"
  vpc_id = var.vpc_id

  ingress {
    from_port       = 9098 # IAM auth port
    to_port         = 9098
    protocol        = "tcp"
    security_groups = var.allowed_security_group_ids
  }

  ingress {
    from_port       = 9092 # plaintext (internal only; prefer 9098)
    to_port         = 9092
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

resource "aws_msk_cluster" "this" {
  cluster_name           = var.cluster_name
  kafka_version          = var.kafka_version
  number_of_broker_nodes = var.broker_count

  broker_node_group_info {
    instance_type   = var.instance_type
    client_subnets  = var.subnet_ids
    security_groups = [aws_security_group.msk.id]

    storage_info {
      ebs_storage_info {
        volume_size = 50
      }
    }
  }

  client_authentication {
    sasl {
      iam = true
    }
  }

  encryption_info {
    encryption_in_transit {
      client_broker = "TLS"
      in_cluster    = true
    }
  }
}

output "bootstrap_brokers" { value = aws_msk_cluster.this.bootstrap_brokers_sasl_iam }
