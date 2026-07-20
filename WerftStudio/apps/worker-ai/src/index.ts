import{startWorker}from"@werft/worker-runtime";
startWorker("ai",async(job)=>{if((process.env.AI_PROVIDER??"disabled")==="disabled")throw Object.assign(new Error("Kein Modellprovider konfiguriert"),{name:"PROVIDER_UNAVAILABLE"});return{runId:job.id,status:"validated",provider:process.env.AI_PROVIDER}});
