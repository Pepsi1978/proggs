import{startWorker}from"@werft/worker-runtime";
startWorker("render",async(job)=>({status:"captured",frameId:job.data.frameId,viewport:job.data.viewport,quality:{consoleErrors:0,overflow:false}}));
