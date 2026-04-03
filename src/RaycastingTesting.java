import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;

import hsa2.GraphicsConsole;

public class RaycastingTesting{

    public static final int ResolutionWidth = 640;
    public static final int ResolutionHeight = 480;
    public static final int ScreenWidth = ResolutionWidth;
    public static final int ScreenHeight = ResolutionHeight;
    public static final int TextureWidth = 64;
    public static final int TextureHeight = 64;
    public static final int NumTextures = 11;

    public static int[][] drawStartEnd = new int[ResolutionWidth][2];
    public static Color[] color = new Color[ResolutionWidth];
    public static double yVelocity = 0;
    public static int height = 0; 

    public static GraphicsConsole gc = new GraphicsConsole(ScreenWidth, ScreenHeight);

    BufferedImage texturePngs[] = new BufferedImage[NumTextures];
    int texture[][] = new int[NumTextures][TextureWidth * TextureHeight];
    int darkerNumber = Integer.parseInt("011111110111111101111111", 2);
    BufferedImage screen;

    double[] zBuffer = new double[ResolutionWidth];

    public static final int NumSprites = 20;
    int[] spriteOrder = new int[NumSprites];
    double[] spriteDistance = new double[NumSprites];

    Sprite[] sprite = {
        new Sprite(20.5, 11.5, 10), //green light in front of playerstart
        //green lights in every room
        new Sprite(18.5,4.5, 10),
        new Sprite(10.0,4.5, 10),
        new Sprite(10.0,12.5,10),
        new Sprite(3.5, 6.5, 10),
        new Sprite(3.5, 20.5,10),
        new Sprite(3.5, 14.5,10),
        new Sprite(14.5,20.5,10),

        //row of pillars in front of wall: fisheye test
        new Sprite(18.5, 10.5, 9),
        new Sprite(18.5, 11.5, 9),
        new Sprite(18.5, 12.5, 9),

        //some barrels around the map
        new Sprite(21.5, 1.5, 8),
        new Sprite(15.5, 1.5, 8),
        new Sprite(16.0, 1.8, 8),
        new Sprite(16.2, 1.2, 8),
        new Sprite(3.5,  2.5, 8),
        new Sprite(9.5, 15.5, 8),
        new Sprite(10.0, 15.1,8),
        new Sprite(10.5, 15.8,8),

        //Player Barrel
        new Sprite(0, 0, 8)
    };

    RaycastingTesting () {
        int mapWidth = 0, mapHeight = 1;
        int[][] map = {};
        File mapFile = new File("src/map.txt");

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
            texturePngs[0] = ImageIO.read(new File("src/images/eagle.png"));
            texturePngs[1] = ImageIO.read(new File("src/images/redbrick.png"));
            texturePngs[2] = ImageIO.read(new File("src/images/purplestone.png"));
            texturePngs[3] = ImageIO.read(new File("src/images/greystone.png"));
            texturePngs[4] = ImageIO.read(new File("src/images/bluestone.png"));
            texturePngs[5] = ImageIO.read(new File("src/images/mossy.png"));
            texturePngs[6] = ImageIO.read(new File("src/images/wood.png"));
            texturePngs[7] = ImageIO.read(new File("src/images/colorstone.png"));
            texturePngs[8] = ImageIO.read(new File("src/images/barrel.png"));
            texturePngs[9] = ImageIO.read(new File("src/images/pillar.png"));
            texturePngs[10] = ImageIO.read(new File("src/images/greenlight.png"));

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
        Vector cameraPos = pos;
        Vector plane = new Vector(0, 0.66);

        double time = 0, oldTime = 0, startTime = System.currentTimeMillis(), numSeconds = 0, numFrames = 0;
        double rotConst = 3.0;

        while (true) {

            screen = new BufferedImage(ResolutionWidth, ResolutionHeight, BufferedImage.TYPE_INT_RGB);

            for (int x = 0; x < ResolutionWidth; x++){
                double cameraX = 2 * x / (double) ResolutionWidth - 1;
                Vector rayDir = dir.addVec(plane.scalMult(cameraX));

                VectorInt mapSquare = new VectorInt((int)cameraPos.x, (int)cameraPos.y);
                Vector sideDist = new Vector();
                Vector deltaDist = new Vector();
                double perpWallDist;
                VectorInt step =  new VectorInt();
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
                    sideDist.x = (cameraPos.x - mapSquare.x) * deltaDist.x;
                } else {
                    step.x = 1;
                    sideDist.x = (mapSquare.x + 1.0 - cameraPos.x) * deltaDist.x;
                }
                if (rayDir.y < 0) {
                    step.y = -1;
                    sideDist.y = (cameraPos.y - mapSquare.y) * deltaDist.y;
                } else {
                    step.y = 1;
                    sideDist.y = (mapSquare.y + 1.0 - cameraPos.y) * deltaDist.y;
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

                int lineHeight = (int)(ResolutionHeight / perpWallDist);

                drawStartEnd[x][0] = (-lineHeight + ResolutionHeight) / 2;
                if (drawStartEnd[x][0] < 0) drawStartEnd[x][0] = 0;
                drawStartEnd[x][1] = (lineHeight + ResolutionHeight) / 2;
                if (drawStartEnd[x][1] >= ResolutionHeight) drawStartEnd[x][1] = ResolutionHeight - 1;
                
                int texNum = map[mapSquare.x][mapSquare.y] - 1;

                double wallX;
                if (!side) wallX = cameraPos.y + perpWallDist * rayDir.y;
                else wallX = cameraPos.x + perpWallDist * rayDir.x;
                wallX -= Math.floor(wallX);

                int texX = (int)(wallX * (double)TextureWidth);
                if (!side && rayDir.x > 0) {
                    texX = TextureWidth - texX - 1;
                } 
                if (side && rayDir.y < 0) {
                    texX = TextureWidth - texX - 1;
                }

                double texStep = (double)TextureHeight / lineHeight;
                double texPos = (drawStartEnd[x][0] - ResolutionHeight/2 + lineHeight/2) * texStep;
                
                for (int y = drawStartEnd[x][0]; y < drawStartEnd[x][1]; y++){
                    int texY = (int)texPos & (TextureHeight - 1);
                    texPos += texStep;
                    int color = texture[texNum][TextureHeight * texY + texX];
                    if (side) color = (color >> 1) & darkerNumber;
                    screen.setRGB(x, y, color);
                }
                zBuffer[x] = perpWallDist;
            }

            sprite[19].setXY(pos);

            for (int i = 0; i < NumSprites; i++){
                spriteOrder[i] = i;
                spriteDistance[i] = ((cameraPos.x - sprite[i].x)*(cameraPos.x - sprite[i].x) + (cameraPos.y - sprite[i].y)*(cameraPos.y - sprite[i].y));
            }

            Sprite.sortSprites(spriteOrder, spriteDistance, NumSprites);

            for (int i = 0; i < NumSprites; i++){
                Vector spriteCamPos = new Vector(sprite[spriteOrder[i]].x - cameraPos.x, sprite[spriteOrder[i]].y - cameraPos.y);
                //transform sprite with the inverse camera matrix
                // [ planeX   dirX ] -1                                       [ dirY      -dirX ]
                // [               ]       =  1/(planeX*dirY-dirX*planeY) *   [                 ]
                // [ planeY   dirY ]                                          [ -planeY  planeX ]
                double invDet = 1.0/(plane.x * dir.y - dir.x * plane.y);
                Vector transform = new Vector(invDet * (dir.y * spriteCamPos.x - dir.x * spriteCamPos.y), invDet * (-plane.y * spriteCamPos.x + plane.x * spriteCamPos.y));
                int spriteScreenX = (int)((ResolutionWidth/2)*(1 + transform.x/transform.y));

                //calculate height of the sprite on screen
                int spriteHeight = Math.abs((int)(ResolutionHeight / transform.y)); //using 'transformY' instead of the real distance prevents fisheye
                //calculate lowest and highest pixel to fill in current stripe
                int drawStartY = -spriteHeight / 2 + ResolutionHeight / 2;
                if(drawStartY < 0) drawStartY = 0;
                int drawEndY = spriteHeight / 2 + ResolutionHeight / 2;
                if(drawEndY >= ResolutionHeight) drawEndY = ResolutionHeight - 1;

                //calculate width of the sprite
                int spriteWidth = Math.abs((int)(ResolutionHeight / transform.y));
                int drawStartX = -spriteWidth / 2 + spriteScreenX;
                if(drawStartX < 0) drawStartX = 0;
                int drawEndX = spriteWidth / 2 + spriteScreenX;
                if(drawEndX >= ResolutionWidth) drawEndX = ResolutionWidth - 1;

                //loop through every vertical stripe of the sprite on screen
                for(int stripe = drawStartX; stripe < drawEndX; stripe++)
                {
                    int texX = (int)(256 * (stripe - (-spriteWidth / 2 + spriteScreenX)) * TextureWidth / spriteWidth) / 256;
                    //the conditions in the if are:
                    //1) it's in front of camera plane so you don't see things behind you
                    //2) it's on the screen (left)
                    //3) it's on the screen (right)
                    //4) ZBuffer, with perpendicular distance
                    if(transform.y > 0 && transform.y < zBuffer[stripe]) {
                        for(int y = drawStartY; y < drawEndY; y++){ //for every pixel of the current stripe
                            int d = (y) * 256 - ResolutionHeight * 128 + spriteHeight * 128; //256 and 128 factors to avoid floats
                            int texY = (int)((((long) d * TextureHeight) / spriteHeight) / 256);
                            int color;
                            //try {
                            color = texture[sprite[spriteOrder[i]].texture][TextureWidth * texY + texX]; //get current color from the texture
                            if((color & 0x00FFFFFF) != 0) screen.setRGB(stripe, y, color); //paint pixel if it isn't black, black is the invisible color
                            /*} catch (Exception e) {
                                System.out.println(e.getMessage());
                                System.out.printf("%d, %f, %d, %d\n", d, transform.y, drawStartY, spriteHeight);
                            }*/
                            
                        }
                    }
                }
            }

            oldTime = time;
            time = System.currentTimeMillis();
            double frameTime = (time - oldTime)/1000;
            numFrames++;

            if ((time - startTime)/1000 > numSeconds){
                System.out.println(numFrames);
                numFrames = 0;
                numSeconds++;
            }

            drawGraphics();

            double moveSpeed = frameTime * 5.0; //the constant value is in squares/second
            double rotSpeed = frameTime * rotConst; //the constant value is in radians/second
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
                rotConst = 0.01;
            }
            if(gc.isKeyDown(69)){
                rotConst = 3.0;
            }

            Vector cameraDir = dir.scalMult(-1);

            VectorInt mapSquare = new VectorInt((int)pos.x, (int)pos.y);
            Vector sideDist = new Vector();
            Vector deltaDist = new Vector();
            VectorInt step =  new VectorInt();
            int hit = 0;
            double perpWallDist, cameraMult;
            boolean side = false;

            if (cameraDir.x == 0){
                deltaDist.x = Double.POSITIVE_INFINITY;
            } else {
                deltaDist.x = Math.abs(Math.sqrt((cameraDir.x)*(cameraDir.x) + (cameraDir.y)*(cameraDir.y))/cameraDir.x);
            }
            if (cameraDir.y == 0){
                deltaDist.y = Double.POSITIVE_INFINITY;
            } else {
                deltaDist.y = Math.abs(Math.sqrt((cameraDir.x)*(cameraDir.x) + (cameraDir.y)*(cameraDir.y))/cameraDir.y);;
            }

            if (cameraDir.x < 0) {
                step.x = -1;
                sideDist.x = (pos.x - mapSquare.x) * deltaDist.x;
            } else {
                step.x = 1;
                sideDist.x = (mapSquare.x + 1.0 - pos.x) * deltaDist.x;
            }
            if (cameraDir.y < 0) {
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
                    side = true;
                } else {
                    sideDist.y += deltaDist.y;
                    mapSquare.y += step.y;
                    side = false;
                }
                if (map[mapSquare.x][mapSquare.y] > 0) hit = 1;
            }

            if(side) perpWallDist = (sideDist.x - deltaDist.x);
            else perpWallDist = (sideDist.y - deltaDist.y);
    
            if (perpWallDist > 1.99) cameraMult = 1.99;
            else cameraMult = perpWallDist;
        
            cameraPos = pos.addVec(cameraDir.scalMult(cameraMult));
            //System.out.println(side + ", " + cameraMult + " ," + perpWallDist + ", " + sideDist.y);

            // cameraPos = pos.addVec(cameraDir.scalMult(1.9));
        }
    }

    public static void main(String[] args) {
        new RaycastingTesting();
    }

    void drawGraphics(){
        gc.drawImage(screen, 0, 0);
    }
}