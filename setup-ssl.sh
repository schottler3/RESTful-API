#!/bin/bash
# SSL Certificate Setup Script

echo "SSL Certificate Setup"
echo "===================="
echo ""
echo "Choose an option:"
echo "1. Generate self-signed certificate (for testing)"
echo "2. Use Let's Encrypt (for production with domain)"
echo "3. Use existing certificates"
echo ""
read -p "Enter option (1-3): " option

case $option in
  1)
    echo "Generating self-signed certificate..."
    mkdir -p ssl
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
      -keyout ssl/key.pem \
      -out ssl/cert.pem \
      -subj "/C=US/ST=State/L=City/O=Organization/CN=localhost"
    echo "Self-signed certificate created in ssl/ directory"
    echo "WARNING: Self-signed certificates should only be used for testing!"
    ;;
  2)
    echo "Let's Encrypt Setup"
    echo "-------------------"
    echo "Prerequisites:"
    echo "- A domain name pointing to this server"
    echo "- Ports 80 and 443 open"
    echo ""
    read -p "Enter your domain name: " domain
    read -p "Enter your email: " email
    
    echo "Installing certbot..."
    if command -v apt-get &> /dev/null; then
      sudo apt-get update
      sudo apt-get install -y certbot
    elif command -v yum &> /dev/null; then
      sudo yum install -y certbot
    else
      echo "Please install certbot manually"
      exit 1
    fi
    
    echo "Obtaining certificate..."
    sudo certbot certonly --standalone -d $domain --email $email --agree-tos --non-interactive
    
    echo "Copying certificates..."
    mkdir -p ssl
    sudo cp /etc/letsencrypt/live/$domain/fullchain.pem ssl/cert.pem
    sudo cp /etc/letsencrypt/live/$domain/privkey.pem ssl/key.pem
    sudo chmod 644 ssl/cert.pem
    sudo chmod 600 ssl/key.pem
    
    echo "Certificate installed successfully!"
    echo "Remember to uncomment HTTPS configuration in nginx.conf"
    ;;
  3)
    echo "Using existing certificates"
    mkdir -p ssl
    echo "Place your certificate files in the ssl/ directory:"
    echo "- ssl/cert.pem (certificate)"
    echo "- ssl/key.pem (private key)"
    echo ""
    echo "After adding certificates, uncomment HTTPS configuration in nginx.conf"
    ;;
  *)
    echo "Invalid option"
    exit 1
    ;;
esac

echo ""
echo "Next steps:"
echo "1. Update nginx.conf to uncomment HTTPS section if using SSL"
echo "2. Update server_name in nginx.conf with your domain"
echo "3. Restart nginx: docker-compose -f docker-compose-prod.yml restart nginx"
