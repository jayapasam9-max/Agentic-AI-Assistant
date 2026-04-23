variable "cluster_name"    { type = string }
variable "cluster_version" { type = string }
variable "vpc_id"          { type = string }
variable "subnet_ids"      { type = list(string) }
variable "node_groups" {
  type = map(object({
    instance_types = list(string)
    min_size       = number
    max_size       = number
    desired_size   = number
  }))
}

module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.24"

  cluster_name    = var.cluster_name
  cluster_version = var.cluster_version
  vpc_id          = var.vpc_id
  subnet_ids      = var.subnet_ids

  cluster_endpoint_public_access = true # tighten to IP allowlist in prod

  # Essential add-ons
  cluster_addons = {
    coredns                = { most_recent = true }
    kube-proxy             = { most_recent = true }
    vpc-cni                = { most_recent = true }
    aws-ebs-csi-driver     = { most_recent = true }
    eks-pod-identity-agent = { most_recent = true }
  }

  eks_managed_node_groups = {
    for k, v in var.node_groups : k => {
      instance_types = v.instance_types
      min_size       = v.min_size
      max_size       = v.max_size
      desired_size   = v.desired_size
    }
  }

  # Enable IRSA for pod-level IAM roles (needed for Secrets Manager CSI, ALB controller, etc.)
  enable_irsa = true
}

output "cluster_name"            { value = module.eks.cluster_name }
output "cluster_endpoint"        { value = module.eks.cluster_endpoint }
output "cluster_oidc_issuer_url" { value = module.eks.cluster_oidc_issuer_url }
output "node_security_group_id"  { value = module.eks.node_security_group_id }
