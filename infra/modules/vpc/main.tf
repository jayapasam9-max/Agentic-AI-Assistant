variable "name"        { type = string }
variable "cidr"        { type = string }
variable "azs"         { type = list(string) }
variable "environment" { type = string }

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.13"

  name = var.name
  cidr = var.cidr
  azs  = var.azs

  # /19 public, /19 private app, /24 DB — standard three-tier layout
  public_subnets   = [cidrsubnet(var.cidr, 3, 0), cidrsubnet(var.cidr, 3, 1), cidrsubnet(var.cidr, 3, 2)]
  private_subnets  = [cidrsubnet(var.cidr, 3, 3), cidrsubnet(var.cidr, 3, 4), cidrsubnet(var.cidr, 3, 5)]
  database_subnets = [cidrsubnet(var.cidr, 8, 192), cidrsubnet(var.cidr, 8, 193), cidrsubnet(var.cidr, 8, 194)]

  enable_nat_gateway = true
  single_nat_gateway = var.environment != "prod" # one NAT for dev/staging, per-AZ for prod

  enable_dns_hostnames = true
  enable_dns_support   = true

  # Tag subnets for EKS auto-discovery
  public_subnet_tags = {
    "kubernetes.io/role/elb" = "1"
  }
  private_subnet_tags = {
    "kubernetes.io/role/internal-elb" = "1"
  }
}

output "vpc_id"              { value = module.vpc.vpc_id }
output "public_subnet_ids"   { value = module.vpc.public_subnets }
output "private_subnet_ids"  { value = module.vpc.private_subnets }
output "database_subnet_ids" { value = module.vpc.database_subnets }
