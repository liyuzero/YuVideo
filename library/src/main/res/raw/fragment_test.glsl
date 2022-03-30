precision mediump float;

const float PI = 3.14159265;
uniform sampler2D inputTexture;

const float uD = 80.0;          //旋转角度参考值
const float uR = 0.5;

varying vec2 textureCoordinate;

void main()
{
    ivec2 ires = ivec2(512, 512);
    float Res = float(ires.y);          //直径

    vec2 st = textureCoordinate;     //纹理采样
    float Radius = Res * uR;            //半径

    vec2 xy = Res * st;                 //将纹理坐标对应到图片坐标

    vec2 dxy = xy - vec2(Res/2., Res/2.);       //获取连接当前图片坐标与图片中心的向量
    float r = length(dxy);      //获取当前图片坐标与图片中心的距离 以此作为旋涡的半径

    float attenValue = (1.0 -(r/Radius)*(r/Radius));        //获取选择因子

    float beta = atan(dxy.y, dxy.x) + radians(uD) * 2.0 * attenValue;   //获取旋转角度

    if(r <= Radius)
    {
        xy = Res/2.0 + r * vec2(cos(beta), sin(beta));  //获取旋转后的坐标
    }

    st = xy/Res;        //将坐标对应回纹理坐标

    vec3 irgb = texture2D(inputTexture, st).rgb;

    gl_FragColor = vec4( irgb, 1.0 );
}