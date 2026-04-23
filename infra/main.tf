terraform {
  required_version = ">= 1.6.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.70"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.32"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.15"
    }
  }

  backend "s3" {
    # Bootstrap the state bucket and lock table manually once, then fill these in.
    # bucket         = "codereview-tfstate-<account_id>"
    # key            = "prod/terraform.tfstate"
    # region         = "us-east-1"
    # dynamodb_table = "codereview-tflock"
    # encrypt        = true
  }
}

provider "aws" {
  region = var.region
  default_tags {
    tags = {
      Project     = "code-review-agent"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

module "vpc" {
  source = "./modules/vpc"

  name        = "${var.project}-${var.environment}"
  cidr        = "10.0.0.0/16"
  azs         = ["${var.region}a", "${var.region}b", "${var.region}c"]
  environment = var.environment
}

module "eks" {
  source = "./modules/eks"

  cluster_name    = "${var.project}-${var.environment}"
  cluster_version = "1.30"
  vpc_id          = module.vpc.vpc_id
  subnet_ids      = module.vpc.private_subnet_ids

  node_groups = {
    agent_workers = {
      instance_types = ["t3.large"]
      min_size       = 2
      max_size       = 10
      desired_size   = 2
    }
  }
}

module "rds" {
  source = "./modules/rds"

  identifier        = "${var.project}-${var.environment}"
  engine_version    = "16.3"
  instance_class    = "db.t4g.medium"
  allocated_storage = 50
  vpc_id            = module.vpc.vpc_id
  subnet_ids        = module.vpc.database_subnet_ids
  # Security group allows ingress from EKS node SG only
  allowed_security_group_ids = [module.eks.node_security_group_id]

  # pgvector is enabled via the shared_preload_libraries parameter group below
  db_name  = "codereview"
  username = "codereview_app"
}

module "msk" {
  source = "./modules/msk"

  cluster_name    = "${var.project}-${var.environment}"
  kafka_version   = "3.7.x"
  broker_count    = 3
  instance_type   = "kafka.t3.small"
  vpc_id          = module.vpc.vpc_id
  subnet_ids      = module.vpc.private_subnet_ids
  allowed_security_group_ids = [module.eks.node_security_group_id]
}

module "ecr" {
  source = "./modules/ecr"

  repositories = ["code-review-agent-backend", "code-review-agent-frontend"]
}

module "observability" {
  source = "./modules/observability"

  cluster_name    = module.eks.cluster_name
  cluster_endpoint = module.eks.cluster_endpoint
  depends_on      = [module.eks]
}

# Store secrets for the backend to pull at runtime via the Secrets Manager CSI driver
resource "aws_secretsmanager_secret" "anthropic_api_key" {
  name = "${var.project}/${var.environment}/anthropic-api-key"
}

resource "aws_secretsmanager_secret" "github_app_private_key" {
  name = "${var.project}/${var.environment}/github-app-private-key"
}

resource "aws_secretsmanager_secret" "github_webhook_secret" {
  name = "${var.project}/${var.environment}/github-webhook-secret"
}
