import{startWorker}from"@werft/worker-runtime";
startWorker("import",async(job)=>{const files=Array.isArray(job.data.files)?job.data.files:[];if(files.some((file:unknown)=>typeof file==="string"&&(file.includes("..")||file.startsWith("/"))))throw Object.assign(new Error("Unsicherer Archivpfad"),{name:"ARCHIVE_UNSAFE"});return{status:"validated",recognized:files.length,blocked:0,warnings:[]}});
