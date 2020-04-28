# Introduction

${polaris_product_name} helps security and development teams analyze security risks
in their software products. The ${solution_name} plugin enables you to invoke ${polaris_product_name}
analysis from your Jenkins builds.
You can use ${solution_name} in both freestyle and pipeline jobs.

When the ${solution_name} plugin runs, it checks the configured
${polaris_product_name} server and the Jenkins node to see if the correct version
of the ${polaris_product_name} Command Line Interpreter (CLI) is installed
on the node. If the ${polaris_product_name} CLI is not installed, the plugin
installs the CLI. In either case, ${solution_name}
then executes the ${polaris_cli_name}, which
analyzes your project, and uploads results to ${polaris_product_name}.
