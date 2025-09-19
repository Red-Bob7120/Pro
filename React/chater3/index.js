import _ from"lodash";


console.log("index is running");

const obj ={
    a:1,
    b:{c:2},
    d:[3,4,{e:5}]
};
//const copy = _.cloneDeep(obj);
const copy = structuredClone(obj);

obj.b.c =99;
obj.d[2].e=100;

console.log(obj);

console.log(copy);

console.log("index is done");