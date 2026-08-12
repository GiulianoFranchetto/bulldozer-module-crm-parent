#
# SPDX-Licence-Identifer: Unlicensed
# SPDX-FileCopyrightText: 2026 Juliano FRANCHETTO <juliano@bulldozer-collective.com>
#

scp -i "/home/giuliano-franchetto/Documents/bulldozer/keys/bulldozer-backend-ec2-keypair.pem" ./module-crm/target/module-crm-1.0-SNAPSHOT.jar  ec2-user@"${BULLDOZER_PRIVATE_IP:-13.38.40.112}":~/crm-module.jar
