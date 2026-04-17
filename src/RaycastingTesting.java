import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import hsa2.GraphicsConsole;

public class RaycastingTesting{

    //Constants
    public static final int ResolutionWidth = 640;
    public static final int ResolutionHeight = 480;
    public static final int ScreenWidth = ResolutionWidth;
    public static final int ScreenHeight = ResolutionHeight;
    public static final int TextureWidth = 64;
    public static final int TextureHeight = 64;
    public static final int MapTextureWidth = 48;
    public static final int MapTextureHeight = 48;
    public static final int SkyTextureWidth = 1000; //Theoretical Optimal is 3447 px wide lol
    public static final int SkyTextureHeight = 480;
    public static final int NumTextures = 11;

    //The graphics console
    public static GraphicsConsole gc = new GraphicsConsole(ScreenWidth, ScreenHeight);

    //Textures
    BufferedImage texturePngs[] = new BufferedImage[NumTextures];
    BufferedImage skyPng, mapPng;
    int texture[][][] = new int[NumTextures][TextureWidth][TextureHeight];
    int mapTexture[][];
    int skyTexture[][];
    int darkerNumber = Integer.parseInt("011111110111111101111111", 2); //bitmask to make colors darker for y-side walls and floor

    //Screen
    BufferedImage screen = new BufferedImage(ResolutionWidth, ResolutionHeight, BufferedImage.TYPE_INT_RGB);
    int[] screenArr = ((DataBufferInt) screen.getRaster().getDataBuffer()).getData();

    //Sprite Variables
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

        //Initilization of map and textures
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
            //System.out.printf("%d, %d", mapHeight, mapWidth);
            map = new int[mapHeight][mapWidth];
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
            skyPng = ImageIO.read(new File("src/images/skyImage.png"));
            mapPng = ImageIO.read(new File("src/images/MapLong.png"));

            for(int i = 0; i < NumTextures; i++){
                for (int y = 0; y < TextureHeight; y++) {
                    for (int x = 0; x < TextureWidth; x++) {
                        texture[i][x][y] = texturePngs[i].getRGB(x,y);
                    }
                }
            }
            mapTexture = new int[mapPng.getWidth()][mapPng.getHeight()];
            for (int y = 0; y < MapTextureHeight; y++) {
                for (int x = 0; x < MapTextureWidth; x++) {
                    mapTexture[MapTextureWidth - x - 1][MapTextureHeight - y - 1] = mapPng.getRGB(x,y);
                }
            }
            skyTexture = new int[skyPng.getWidth()][skyPng.getHeight()];
            for (int y = 0; y < SkyTextureHeight; y++) {
                for (int x = 0; x < SkyTextureWidth; x++) {
                    skyTexture[x][y] = skyPng.getRGB(x, y);
                }
            }
            
        } catch (IOException e) {
            System.out.println("Problem Reading File.");
        }

        //Player Variables
        Vector pos = new Vector(22, 12);
        Vector dir = new Vector(-1, 0);
        Vector cameraPos = pos;
        Vector plane = new Vector(0,0.66);

        //Time Variables
        double time = 0, oldTime = 0, startTime = System.currentTimeMillis(), numSeconds = 0, numFrames = 0;
        
        //Sky Render Variables
        double skyPixelsPerRevolution = SkyTextureWidth / (2 * Math.PI);
        double angBetwRays = Math.atan2(plane.y, -dir.x) * 2 / ScreenWidth;
        double skyStepX = SkyTextureWidth/(2*Math.PI/angBetwRays);
        
        //Game Loop
        while (true) {

            //Render Floor
            double dirAng = Math.atan2(dir.y, dir.x);
            Vector rayDir0 = new Vector(dir.x - plane.x, dir.y - plane.y);
            Vector rayDir1 = new Vector(dir.x + plane.x, dir.y + plane.y);

            for (int y = ResolutionHeight/2; y < ResolutionHeight; y++) {

                int p = y - ResolutionHeight / 2;
                double posZ = 0.5 * ResolutionHeight;
                double rowDistance = posZ / p;
                
                Vector floorStep = new Vector(rowDistance * (rayDir1.x - rayDir0.x) / ResolutionWidth, rowDistance * (rayDir1.y - rayDir0.y) / ResolutionWidth);
                Vector floor = new Vector(cameraPos.x + rowDistance * rayDir0.x, cameraPos.y + rowDistance * rayDir0.y);

                for (int x = 0; x < ResolutionWidth; x++){
                    VectorInt cell = new VectorInt((int)(floor.x), (int)(floor.y));
                    VectorInt fcTexture = new VectorInt(Math.abs((int)(MapTextureWidth * (floor.y / mapWidth - cell.y)) % (MapTextureWidth)), Math.abs((int)(MapTextureHeight * (floor.x / mapHeight - cell.x)) % (MapTextureHeight)));

                    floor.x += floorStep.x;
                    floor.y += floorStep.y;

                    int color;
                    color = mapTexture[fcTexture.x][fcTexture.y];
                    color = (color >> 1) & darkerNumber;
                    screenArr[y * ResolutionWidth + x] = color;
                }
            }

            //Render Walls and Sky
            for (int x = 0; x < ResolutionWidth; x++){
                //Calculate ray position and direction
                double cameraX = 2 * x / (double) ResolutionWidth - 1;
                Vector rayDir = dir.addVec(plane.scalMult(cameraX));

                //Variable initialization for DDA
                VectorInt mapSquare = new VectorInt((int)cameraPos.x, (int)cameraPos.y);
                Vector sideDist = new Vector();
                Vector deltaDist = new Vector();
                double perpWallDist;
                VectorInt step =  new VectorInt();
                int hit = 0;
                boolean side = false; //NS = true, EW = false;

                //Calculate step and initial sideDist
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

                //Perform DDA
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

                //Calculate line's parameters to draw on screen
                int lineHeight = (int)(ResolutionHeight / perpWallDist);
                int drawStart, drawEnd;

                drawStart = (-lineHeight + ResolutionHeight) / 2;
                if (drawStart < 0) drawStart = 0;
                drawEnd = (lineHeight + ResolutionHeight) / 2;
                if (drawEnd >= ResolutionHeight) drawEnd = ResolutionHeight - 1;
                
                //Wall Texture Calculations
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
                double texPos = (drawStart - ResolutionHeight/2 + lineHeight/2) * texStep;

                //Render Wall
                for (int y = drawStart; y < drawEnd; y++) {
                    int texY = (int)texPos & (TextureHeight - 1);
                    texPos += texStep;
                    int color = texture[texNum][texX][texY];
                    if (side) color = (color >> 1) & darkerNumber;
                    screenArr[y * ResolutionWidth + x] = color;
                }

                //Render Sky
                int skyX = (int)(dirAng * skyPixelsPerRevolution - x * skyStepX);
                if (skyX < 0) skyX += SkyTextureWidth;
                else if (skyX >= SkyTextureWidth) skyX -= SkyTextureWidth;

                for (int y = 0; y < drawStart; y++) {
                    int color = skyTexture[skyX][y];
                    screenArr[y * ResolutionWidth + x] = color;
                }

                //Set ZBuffer for sprite rendering
                zBuffer[x] = perpWallDist;
            }

            //Sprite Rendering
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
                    int texX = (int)(256 * (long)(stripe - (-spriteWidth / 2 + spriteScreenX)) * TextureWidth / spriteWidth) / 256;
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
                            color = texture[sprite[spriteOrder[i]].texture][texX][texY]; //get current color from the texture
                            if((color & 0x00FFFFFF) != 0) screenArr[y * ResolutionWidth + stripe] = color; //paint pixel if it isn't black, black is the invisible color
                            
                        }
                    }
                }
            }

            //Timing
            oldTime = time;
            time = System.currentTimeMillis();
            double frameTime = (time - oldTime)/1000;
            numFrames++;

            //Print framerate in console every second
            if ((time - startTime)/1000 > numSeconds){
                System.out.println(numFrames);
                numFrames = 0;
                numSeconds++;
            }

            drawGraphics();

            //Movement
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

            //Haha funny plane skewing (makes for very funny visual effects)
            if(gc.isKeyDown(81)){
                plane.y += 0.01;
            }
            if(gc.isKeyDown(69)){
                plane.y -= 0.01;
            }

            //Camera Collision Detection (prevents camera from going through walls when close to them)
            Vector cameraDir = dir.scalMult(-1);

            VectorInt mapSquare = new VectorInt((int)pos.x, (int)pos.y);
            Vector sideDist = new Vector();
            Vector deltaDist = new Vector();
            VectorInt step =  new VectorInt();
            int hit = 0;
            double perpWallDist, cameraMult;
            boolean side = false;
            double cameraMag = cameraDir.getMagnitude();

            if (cameraDir.x == 0){
                deltaDist.x = Double.POSITIVE_INFINITY;
            } else {
                deltaDist.x = Math.abs(cameraMag/cameraDir.x);
            }
            if (cameraDir.y == 0){
                deltaDist.y = Double.POSITIVE_INFINITY;
            } else {
                deltaDist.y = Math.abs(cameraMag/cameraDir.y);
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
                //System.out.println(mapSquare.x + ", " + mapSquare.y);
                if (map[mapSquare.x][mapSquare.y] > 0) hit = 1;
            }

            if(side) perpWallDist = (sideDist.x - deltaDist.x);
            else perpWallDist = (sideDist.y - deltaDist.y);
    
            if (perpWallDist > 2) cameraMult = 2;
            else cameraMult = perpWallDist - 0.01;
        
            cameraPos = pos.addVec(cameraDir.scalMult(cameraMult));
            
            //System.out.printf("%f, %f\n", cameraPos.x, cameraPos.y);
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