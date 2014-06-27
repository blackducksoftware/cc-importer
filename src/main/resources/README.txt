Code Center Importer program, 0.7

Provides the ability to import all or a specific list of projects from Protex to Code Center.

USAGE:
java -jar ccImporter.jar [-getProjectList | -importProjectList [aSingleProject,version] | -importAllProjects]

-getProjectList : Displays a list of projects in Protex assigned to the user
-importProjectList [aSingleProject,version] : Imports the projects specified in the context.xml file into Code Center, or a single project specified in the command line.
-importAllProjects : Imports all user assigned Protex projects into Code Center.

PRE-REQUISITES:
1. Both Protex SDK and Code Center SDK need to be enabled.

2. Protex server needs to be configured in Code Center.

3. Create ccimporter.properties file: replace values including brackets
--------------------------------
#IF using -D to the VM to pass in the properties, remove the entire key/value pair
env.protex.server=[PROTEX SERVER URL]
env.protex.user=[PROTEX USERNAME]
env.protex.password=[PROTEX PASSWORD]

env.cc.server=[CC SERVER URL]
env.cc.user=[CC USERNAME]
env.cc.password=[CC PASSWORD]

# Name given to the Protex Server connection in Code Center (Administration > Settings > Protex)
cc.protex.name=[CC PROTEX CONNECTION NAME]
# Default application version assigned to CC applications if none is specified in the input
cc.default.app.version=Unspecified
# Name of workflow to associate with project
cc.workflow=[CC workflow]
# Username of Code Center application owner
cc.owner=[CC APP OWNER]
#  true/false if component requests should be submitted. 
cc.submit.request=true

#List of Protex projects names to import into Code Center.  
protex.project.list=[PROTEX PROJECT LIST]
--------------------------------		

KNOWN ISSUES:
- If a Protex project's Bill of Materials contains a component that is a license, the import for that project will fail.
- If the request process in Code Center contains custom required fields, or if the approvers are set at request time, set the submitRequest to false.
- If the Protex project is already associated to a Code Center project, the import will fail.
	