import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.image.BufferedImage;

import hsa2.GraphicsConsole;

public class RaycastingTesting{

    public static final int ScreenWidth = 640;
    public static final int ScreenHeight = 480;
    public static final int TextureWidth = 64;
    public static final int TextureHeight = 64;
    public static final int NumTextures = 11;

    public static int currentX;
    public static int[][] drawStartEnd = new int[ScreenWidth][2];
    public static Color[] color = new Color[ScreenWidth];
    public static double yVelocity = 0;
    public static int height = 0; 
    public static boolean jumped = false;

    public static GraphicsConsole gc = new GraphicsConsole(ScreenWidth, ScreenHeight);

    BufferedImage texturePngs[] = new BufferedImage[NumTextures];
    int texture[][] = new int[NumTextures][TextureWidth * TextureHeight];
    int darkerNumber = Integer.parseInt("011111110111111101111111", 2);
    BufferedImage screen = new BufferedImage(ScreenWidth, ScreenHeight, BufferedImage.TYPE_INT_RGB);
    
    RaycastingTesting () {
        int mapWidth = 0, mapHeight = 1;
        int[][] map = {};
        File mapFile = new File("Personal Testing\\src\\map.txt");

        gc.setBackgroundColor(Color.BLACK);

        try {
            FileReader r = new FileReader(mapFile);
            BufferedReader reader = new BufferedReader(r);
            mapWidth = reader.readLine().length();
            while (reader.readLine() != null){
                mapHeight++;
            }
            reader.close();
            r.close();
            r = new FileReader(mapFile);
            reader = new BufferedReader(r);
            map = new int[mapWidth][mapHeight];
            for (int i = 0; i < mapHeight; i++){
                for (int j = 0; j < mapWidth; j++){
                    map[i][j] = reader.read() - 48;
                }
                reader.readLine();
            }
            reader.close();
            r.close();
            texturePngs[0] = ImageIO.read(new File("Personal Testing\\src\\images\\eagle.png"));
            texturePngs[1] = ImageIO.read(new File("Personal Testing\\src\\images\\redbrick.png"));
            texturePngs[2] = ImageIO.read(new File("Personal Testing\\src\\images\\purplestone.png"));
            texturePngs[3] = ImageIO.read(new File("Personal Testing\\src\\images\\greystone.png"));
            texturePngs[4] = ImageIO.read(new File("Personal Testing\\src\\images\\bluestone.png"));
            texturePngs[5] = ImageIO.read(new File("Personal Testing\\src\\images\\mossy.png"));
            texturePngs[6] = ImageIO.read(new File("Personal Testing\\src\\images\\wood.png"));
            texturePngs[7] = ImageIO.read(new File("Personal Testing\\src\\images\\colorstone.png"));
            texturePngs[8] = ImageIO.read(new File("Personal Testing\\src\\images\\barrel.png"));
            texturePngs[9] = ImageIO.read(new File("Personal Testing\\src\\images\\pillar.png"));
            texturePngs[10] = ImageIO.read(new File("Personal Testing\\src\\images\\greenlight.png"));

            for(int i = 0; i < NumTextures; i++){
                for (int y = 0; y < TextureHeight; y++) {
                    for (int x = 0; x < TextureWidth; x++) {
                        texture[i][TextureHeight * y + x] = texturePngs[i].getRGB(x,y);
                    }
                }
            }
            
        } catch (IOException e) {
            System.out.println("Problem Reading File.");
        }

        Vector pos = new Vector(22, 12);
        Vector dir = new Vector(-1, 0);
        Vector plane = new Vector(0, 0.66);

        double time = 0, oldTime = 0;

        while (true) {

            for (int x = 0; x < ScreenWidth; x++){
                currentX = x;
                double cameraX = 2 * x / (double) ScreenWidth - 1;
                Vector rayDir = dir.addVec(plane.scalMult(cameraX));

                VectorInt mapSquare = new VectorInt((int)pos.x, (int)pos.y);
                Vector sideDist = new Vector();
                Vector deltaDist = new Vector();
                double perpWallDist;
                VectorInt step =  new VectorInt(); //False for left, True for right.
                int hit = 0;
                boolean side = false; //NS = true, EW = false;

                if (rayDir.x == 0){
                    deltaDist.x = Double.POSITIVE_INFINITY;
                } else {
                    deltaDist.x = Math.abs(1/rayDir.x);
                }
                if (rayDir.y == 0){
                    deltaDist.y = Double.POSITIVE_INFINITY;
                } else {
                    deltaDist.y = Math.abs(1/rayDir.y);
                }

                if (rayDir.x < 0) {
                    step.x = -1;
                    sideDist.x = (pos.x - mapSquare.x) * deltaDist.x;
                } else {
                    step.x = 1;
                    sideDist.x = (mapSquare.x + 1.0 - pos.x) * deltaDist.x;
                }
                if (rayDir.y < 0) {
                    step.y = -1;
                    sideDist.y = (pos.y - mapSquare.y) * deltaDist.y;
                } else {
                    step.y = 1;
                    sideDist.y = (mapSquare.y + 1.0 - pos.y) * deltaDist.y;
                }

                while (hit == 0){
                    if (sideDist.x < sideDist.y) {
                        sideDist.x += deltaDist.x;
                        mapSquare.x += step.x;
                        side = false;
                    } else {
                        sideDist.y += deltaDist.y;
                        mapSquare.y += step.y;
                        side = true;
                    }
                    if (map[mapSquare.x][mapSquare.y] > 0) hit = 1;
                }

                if (!side) perpWallDist = sideDist.x - deltaDist.x;
                else perpWallDist = sideDist.y - deltaDist.y;
                //if (!side) perpWallDist = sideDist.x;
                //else perpWallDist = sideDist.y;

                int lineHeight = (int)(ScreenHeight / perpWallDist);

                drawStartEnd[x][0] = (-lineHeight + ScreenHeight) / 2;
                if (drawStartEnd[x][0] < 0) drawStartEnd[x][0] = 0;
                drawStartEnd[x][1] = (lineHeight + ScreenHeight) / 2;
                if (drawStartEnd[x][1] >= ScreenHeight) drawStartEnd[x][1] = ScreenHeight - 1;
                
                int texNum = map[mapSquare.x][mapSquare.y] - 1;

                double wallX;
                if (!side) wallX = pos.y + perpWallDist * rayDir.y;
                else wallX = pos.x + perpWallDist * rayDir.x;
                wallX -= Math.floor(wallX);

                int texX = (int)(wallX * (double)TextureWidth);
                if (!side && rayDir.x > 0) {
                    texX = TextureWidth - texX - 1;
                } 
                if (side && rayDir.y < 0) {
                    texX = TextureWidth - texX - 1;
                }

                double texStep = (double)TextureHeight / lineHeight;
                double texPos = (drawStartEnd[x][0] - ScreenHeight/2 + lineHeight/2) * texStep;
                for (int y = 0; y < ScreenHeight; y++){
                    screen.setRGB(x, y, 0);
                }
                for (int y = drawStartEnd[x][0]; y < drawStartEnd[x][1]; y++){
                    int texY = (int)texPos & (TextureHeight - 1);
                    texPos += texStep;
                    int color = texture[texNum][TextureHeight * texY + texX];
                    if (side) color = (color >> 1) & darkerNumber;
                    screen.setRGB(x, y, color);
                }

                /*switch(map[mapSquare.x][mapSquare.y]){
                    case 1: 
                        color[x] = Color.RED;
                        break;
                    case 2:
                        color[x] = Color.GREEN;
                        break;
                    case 3:
                        color[x] = Color.BLUE;
                        break;
                    case 4:
                        color[x] = Color.WHITE;
                        break;
                    default:
                        color[x] = Color.YELLOW;
                        break;
                }

                if (side) {
                    color[x] = color[x].darker();
                }*/
            }

            oldTime = time;
            time = System.currentTimeMillis();
            double frameTime = (time - oldTime)/1000;
            //System.out.println(1.0/frameTime);

            drawGraphics();

            double moveSpeed = frameTime * 5.0; //the constant value is in squares/second
            double rotSpeed = frameTime * 3.0; //the constant value is in radians/second
            //move forward if no wall in front of you
            if(gc.isKeyDown(87)) {
                if(map[(int)(pos.x + dir.x * moveSpeed)][(int)pos.y] == 0) pos.x += dir.x * moveSpeed;
                if(map[(int)pos.x][(int)(pos.y + dir.y * moveSpeed)] == 0) pos.y += dir.y * moveSpeed;
            }
            //move backwards if no wall behind you
            if(gc.isKeyDown(83)) {
                if(map[(int)(pos.x - dir.x * moveSpeed)][(int)(pos.y)] == 0) pos.x -= dir.x * moveSpeed;
                if(map[(int)(pos.x)][(int)(pos.y - dir.y * moveSpeed)] == 0) pos.y -= dir.y * moveSpeed;
            }
            //rotate to the right
            if(gc.isKeyDown(68)) {
                //both camera direction and camera plane must be rotated
                double olddirX = dir.x;
                dir.x = dir.x * Math.cos(-rotSpeed) - dir.y * Math.sin(-rotSpeed);
                dir.y = olddirX * Math.sin(-rotSpeed) + dir.y * Math.cos(-rotSpeed);
                double oldplaneX = plane.x;
                plane.x = plane.x * Math.cos(-rotSpeed) - plane.y * Math.sin(-rotSpeed);
                plane.y = oldplaneX * Math.sin(-rotSpeed) + plane.y * Math.cos(-rotSpeed);
            }
            //rotate to the left
            if(gc.isKeyDown(65)) {
                //both camera direction and camera plane must be rotated
                double olddirX = dir.x;
                dir.x = dir.x * Math.cos(rotSpeed) - dir.y * Math.sin(rotSpeed);
                dir.y = olddirX * Math.sin(rotSpeed) + dir.y * Math.cos(rotSpeed);
                double oldplaneX = plane.x;
                plane.x = plane.x * Math.cos(rotSpeed) - plane.y * Math.sin(rotSpeed);
                plane.y = oldplaneX * Math.sin(rotSpeed) + plane.y * Math.cos(rotSpeed);
            }

            if(gc.isKeyDown(81)){
                plane.y += 0.01;
            }
            if(gc.isKeyDown(69)){
                plane.y -= 0.01;
            }
            /*
            if(gc.isKeyDown(32)){
                if (!jumped){
                    jumped = true;
                    yVelocity += 5;
                }
            } else {
                jumped = false;
            }
            if (height < 0){
                height = 0;
                yVelocity = 0;
            } else {
                height += yVelocity;
            }
            yVelocity -= 0.3;
            height += (int)(yVelocity);
            */
        }
    }

    public static void main(String[] args) {
        new RaycastingTesting();
    }

    void drawGraphics(){
        synchronized(gc){
            gc.clear();
            /*for (int i = 0; i < ScreenWidth; i++){
                gc.setColor(color[i]);
                gc.drawLine(i, drawStartEnd[i][0] + height, i, drawStartEnd[i][1] + height);
            }*/
            gc.drawImage(screen, 0, 0);
        }
    }
}