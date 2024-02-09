#!/bin/bash

# OBJECTIF :
#
# Générer une collection des instance (graphes) aléatoires.

# PARAMÈTRES DU SCRIPT (optionnels) :
#
# -r Donner le nom du répertoire sur lequel les nouvelles instances vont
#	 être enregistrées.
# -n Les différentes instances générées avec ce script contiennent un nombre
#	 des sommets différent. Ce paramètre précise le nombre des sommets à
#	 utiliser dans un format MIN:MAX:PAS.
#	 Exemple : 20:50:10 va produire des graphes avec 20, 30, 40 et 50 sommets.
# -d Il s'agit d'un paramètre lié à la génération aléatoire des arêtes et
#	 correspond à la densité du graphe. Plus précisément, une arête existe avec
#	 une probabilité d, ou autrement dit le graphe généré contiendra
#	 approximativement d% des arêtes possibles. La valeur de ce paramètre varie
#	 pour les différentes instances et est donnée dans un format MIN:MAX:PAS.
#	 Exemple : 50:70:10 va générer des graphes avec densité 50%, 60%, et 70%.
# -i Décrive le nombre de différentes instances qui vont être générées
#	 pour chaque combinaison de paramètres n et d.


# FONCTION USAGE
#
# Affiche une aide sur l'utilisation du script et termine son exécution.

usage () {
	echo ""
	echo "NAME"
	echo -e "\t generate_collection\n"
	echo "DESCRIPTION"
	echo -e "\t Generates a collection of graphes.\n"
	echo "USAGE"
	echo -e "\t bash generate_collection.sh [OPTIONS]\n"
	echo "OPTIONS"
	echo -e "\t -r DIR_NAME"
	echo -e "\t\t set the name of the created directory which will contain the collection;"
	echo -e "\t\t default value: collection\n"
	echo -e "\t -n MIN:MAX:STEP"
	echo -e "\t\t set the limites and the step concerning the number of vertices that"
	echo -e "\t\t will be created in the generated instances;"
	echo -e "\t\t default values: 20:100:20\n"
	echo -e "\t -d MIN:MAX:STEP"
	echo -e "\t\t set the limites and the step concerning the density parameter that will be"
	echo -e "\t\t used in the generation of the edges;"
	echo -e "\t\t default values: 60:90:10\n"
	echo -e "\t -i NUM"
	echo -e "\t\t create NUM instances for each combination of the parameters n and d;"
	echo -e "\t\t default value: 10\n"
	echo -e "\t -h"
	echo -e "\t\t display this help and exit\n"
	echo "EXAMPLE"
	echo -e "\t bash generate_collection.sh -n 50:200:50 -r boo\n"
	echo -e "\t Creates in the directory boo 10 graphes (default value) for each"
	echo -e "\t combination of parameters d={60,70,80,90} (default value)"
	echo -e "\t and n={50,100,150,200}\n"
	exit 1
}


# DÉFINITION DE PARAMÈTRES PAR DÉFAUT
#
# On définit d'abord les valeurs par défaut des paramètres qui vont être
# utilisées dans le cas où l'utilisateur ne donne pas la valeur de certains
# ou de tous les paramètres.

dir="collection"
nb_vertices_min=20
nb_vertices_max=100
nb_vertices_step=20
d_min=60
d_max=90
d_step=10
nb_instances=10


# GESTION DE PARAMÈTRES DONNÉS PAR L'UTILISATEUR
#
# Cherchez et utilisez la commande "getopts" (et pas "getopt").
# L'objectif est de mettre éventuellement à jour les paramètres par défaut,
# si l'utilisateur a choisi d'utiliser des valeurs différentes.
#
# Supprimez le # dans les deux lignes suivantes :
#		usage
#		exit 0
# et ensuite enregistrez et exécutez le script avec la commande :
# 	bash generate_collection.sh
# afin d'avoir une idée des différents cas à traiter.
# N'oubliez pas à remettre les # dans les deux lignes !

# TODO


# GÉNÉRATION DES INSTANCES ALÉATOIRES
#
# Créez dans le répertoire dir des instances aléatoires avec les paramètres
# déjà définis : pour chaque couple de valeurs ($nb_sommets, $d), il faut créer
# $nb_instances instances en appelant le script generate_instance.sh.
#
# ATTENTION : NOM DU FICHIER DE CHAQUE INSTANCE
#
# Il faut obligatoirement respecter le format suivant :
#		nb_sommets_d_instance
#
# Exemple : l'instance numéro 4 avec 20 sommets et d=70 sera enregistrer
# au fichier (sans suffixe, des espaces ou d'autres caractères) :
#		20_70_4

# TODO