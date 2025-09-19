const PT = 3.141592;


function getArea(radius){
    return PT* radius*radius;
}
function getCircumferenc(radius){
    return 2*PT*radius;
}

export const testvVal = 10;
export default{ PT,getArea,getCircumferenc }