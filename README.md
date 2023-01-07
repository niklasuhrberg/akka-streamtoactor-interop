# Sample applications

Two sample applications showing different approaches to stream to actor interoperability in Akka streams.  

## Ask based 
A simple flow with actor interoperability using the ask pattern together with `ActorFlow.ask`

The code is prepared to enable simple modification to study different error scenarios, look at the `var counter`. 

## Stage actor based

A simple flow with actor interoperability using the stage actor based approach using `ActorRefBackpressureProcessFlowStage`
The code is prepared to enable simple modification to study different error scenarios, look at the `var counter`.

# Running instructions

In an sbt shell: 
To run the stageactor based demo application issue:
`run` 
or
`runMain com.triadicsystems.examples.withstageactor.Main`

To run the ask based demo application issue:
`runMain com.triadicsystems.examples.withask.Main`