package OnLattice2DCells;

import HAL.GridsAndAgents.AgentGrid2D;
import HAL.GridsAndAgents.AgentList;
import HAL.GridsAndAgents.AgentSQ2Dunstackable;
import HAL.Gui.GridWindow;
import HAL.Rand;
import HAL.Util;
import java.lang.Math;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import HAL.Gui.GifMaker;

//Author: Hannah Simon, hannahgsimon on Git

class CellFunctions extends AgentSQ2Dunstackable<OnLattice2DGrid>
{
    Type type;
    int color;
    int radiationDose;
    double dieProb;
    double dieProbRad;
    double dieProbImm;
    double divProb;

    public enum Type
    {
        LYMPHOCYTE,
        TUMOR,
        DOOMEDRAD,
        DOOMEDIMM,
        TRIGGERING
    }

    public void Init (Type type)
    {
        this.type = type;
        this.radiationDose = OnLattice2DGrid.baseRadiationDose;
        if (type == Type.LYMPHOCYTE)
        {
            this.color = Util.CategorialColor(Lymphocytes.colorIndex);
            this.dieProb = Lymphocytes.dieProb;
            this.dieProbRad = 0; this.dieProbImm = 0; this.divProb = 0;
        }
        else if (type == Type.TUMOR)
        {
            this.color = Util.CategorialColor(TumorCells.colorIndex);
            this.dieProb = 0;
            this.dieProbRad = TumorCells.dieProbRad;
            this.dieProbImm = TumorCells.dieProbImm;
            this.divProb = TumorCells.divProb;
        }
        else if (type == Type.TRIGGERING)
        {
            this.color = Util.CategorialColor(TriggeringCells.colorIndex);
        }
    }

    public void InitDoomed (boolean radiation)
    {
        this.color = Util.CategorialColor(DoomedCells.colorIndex);
        this.dieProb = DoomedCells.dieProb;
        this.dieProbRad = 0; this.dieProbImm = 0; this.divProb = 0;
        if (radiation)
        {
            this.type = Type.DOOMEDRAD;
        }
        else
        {
            this.type = Type.DOOMEDIMM;
        }
    }

    public void StepCell()
    {
        if (this.type == Type.LYMPHOCYTE)
        {
            if (G.rng.Double() < this.dieProb)
            {
                Lymphocytes.count--;
                Dispose();
            }
        }

        else if (this.type == Type.TUMOR)
        {
            if (G.rng.Double() < this.dieProbRad)
            {
                this.InitDoomed(true);
                TumorCells.count--;
                DoomedCells.countRad++;
            }
            else if (G.rng.Double() < (this.dieProbRad + this.dieProbImm))
            {
                this.InitDoomed(false);
                TumorCells.count--;
                DoomedCells.countImm++;
            }
            else if (G.rng.Double() < (this.dieProbRad + this.dieProbImm + this.divProb))
            {
                mapEmptyHood();
            }
        }

        else if (this.type == Type.DOOMEDRAD)
        {
            if (G.rng.Double() < this.dieProb)
            {
                Dispose();
                DoomedCells.countRad--;
            }
        }
        else if (this.type == Type.DOOMEDIMM)
        {
            if (G.rng.Double() < this.dieProb)
            {
                Dispose();
                DoomedCells.countImm--;
            }
        }

        else if (this.type == Type.TRIGGERING)
        {

        }

    }

    public void mapEmptyHood()
    {
        int options = MapEmptyHood(G.divHood);
        if (options > 0)
        {
            G.NewAgentSQ(G.divHood[G.rng.Int(options)]).Init(Type.TUMOR);
            TumorCells.count++;
        }
    }

    public void lymphociteMigration(List<int[]> availableSpaces, OnLattice2DGrid G, boolean migration, int lymphocitePopulation) throws Exception
    {
        int newLymphocytes = 0;
        if (migration)
        {
            newLymphocytes = (int) (Lymphocytes.tumorInfiltrationRate * TumorCells.count);
            //newLymphocytes = (int) (Lymphocytes.tumorInfiltrationRate * TumorCells.count + getRadiationReducedInfiltration(Lymphocytes.fullName));
        }
        else if (!migration)
        {
            newLymphocytes = lymphocitePopulation;
        }

        int spacesToPick = Math.min(newLymphocytes, availableSpaces.size()); // Ensure we don’t pick more spaces than available
        Collections.shuffle(availableSpaces);

        for (int i = 0; i < spacesToPick; i++)
        {
            G.NewAgentSQ(availableSpaces.get(i)[0], availableSpaces.get(i)[1]).Init(Type.LYMPHOCYTE);
        }

        Lymphocytes.count += spacesToPick;
    }

    public static double getDecayConstant(String className, String decayConstant) throws Exception
    {
        try
        {
            Class<?> clazz = Class.forName(className);
            Field field = clazz.getDeclaredField(decayConstant);
            Object value = field.get(null); // Static field, use 'null' for static access
            return (Double) value;
        }
        catch (ClassNotFoundException e)
        {
            System.err.println("Class not found: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        catch (NoSuchFieldException e)
        {
            System.err.println("Field not found: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        catch (Exception e)
        {
            System.err.println("Error during reflection: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public static double getSurvivingFraction(double radiationDose, String className, String alpha, String beta) throws Exception
    {
        try
        {
            Class<?> clazz = Class.forName(className);

            Field field1 = clazz.getDeclaredField(alpha);
            Object value1 = field1.get(null); // Static field, use 'null' for static access

            Field field2 = clazz.getDeclaredField(beta);
            Object value2 = field2.get(null);

            return Math.exp((Double) value1 * -radiationDose - (Double) value2 * Math.pow(radiationDose, 2));
            //return (Double) (-x.radiationSensitivityOfLymphocytesAlpha * radiationDose - radiationSensitivityOfLymphocytesBeta * Math.pow(radiationDose, 2));
        }
        catch (ClassNotFoundException e)
        {
            System.err.println("Class not found: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        catch (NoSuchFieldException e)
        {
            System.err.println("Field not found: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        catch (Exception e)
        {
            System.err.println("Error during reflection: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public static double getTumorInfiltrationRate(String className) throws Exception
    {
        try
        {
            Class<?> clazz = Class.forName(className);
            Field field = clazz.getDeclaredField("tumorInfiltrationRate");
            Object value = field.get(null); // Static field, use 'null' for static access
            return (Double) value;
        }
        catch (ClassNotFoundException e)
        {
            System.err.println("Class not found: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        catch (NoSuchFieldException e)
        {
            System.err.println("Field not found: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        catch (Exception e)
        {
            System.err.println("Error during reflection: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public static double getPrimaryImmuneResponse(String className) throws Exception
    {
        try
        {
            double primaryImmuneResponse;
            double concentrationAntiPD1_PDL1 = 0;

            Class<?> clazz = Class.forName(className);

            Field field1 = clazz.getDeclaredField("rateOfCellKilling");
            Object value1 = field1.get(null);

            Field field2 = clazz.getDeclaredField("immuneSuppressionEffect");
            Object value2 = field2.get(null);

            return (((Double) value1 * Lymphocytes.count) / (1 + (((Double) value2 * Math.pow(TumorCells.count, ((double) 2 / 3)) * Lymphocytes.count) / (1 + concentrationAntiPD1_PDL1))));
        }
        catch (ClassNotFoundException e)
        {
            System.err.println("Class not found: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        catch (NoSuchFieldException e)
        {
            System.err.println("Field not found: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        catch (Exception e)
        {
            System.err.println("Error during reflection: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public static double getSecondaryImmuneResponse(String className)
    {
        double secondaryImmuneResponse = 0;
        double concentrationAntiCTLA4 = 0;
        double sensitivityFactorZs;

//        for (int i = 0;, i < timestep; i++)
//        {
//            secondaryImmuneResponse += sensitivityFactorZs * ((1 + concentrationAntiCTLA4) / (1 + concentrationAntiCTLA4)) *
//        }
        return secondaryImmuneResponse;
    }

    public static double getTumorGrowthRate(String className) throws Exception
    {
        try
        {
            Class<?> clazz = Class.forName(className);
            Field field = clazz.getDeclaredField("tumorGrowthRate");
            Object value = field.get(null);
            return (Double) value;
        }
        catch (ClassNotFoundException e)
        {
            System.err.println("Class not found: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        catch (NoSuchFieldException e)
        {
            System.err.println("Field not found: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        catch (Exception e)
        {
            System.err.println("Error during reflection: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public static double getLymphocytesProb(int radiationDose) throws Exception
    {
        try
        {
            Lymphocytes.survivingFractionL = CellFunctions.getSurvivingFraction(radiationDose, Lymphocytes.fullName, "radiationSensitivityOfLymphocytesAlpha", "radiationSensitivityOfLymphocytesBeta");
            return 1 - Lymphocytes.survivingFractionL + (Lymphocytes.survivingFractionL * Lymphocytes.decayConstantOfL);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static double getRadiationReducedInfiltration(String className) throws Exception
    {
        try
        {
            Class<?> clazz = Class.forName(className);
            Field field = clazz.getDeclaredField("radiationReducedInfiltration");
            Object value = field.get(null);
            return (Double) value;
        }
        catch (ClassNotFoundException e)
        {
            System.err.println("Class not found: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        catch (NoSuchFieldException e)
        {
            System.err.println("Field not found: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        catch (Exception e)
        {
            System.err.println("Error during reflection: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public static double[] getTumorCellsProb(int radiationDose) throws Exception
    {
        try
        {
            double primaryImmuneResponse = CellFunctions.getPrimaryImmuneResponse(TumorCells.fullName);
            TumorCells.survivingFractionT = CellFunctions.getSurvivingFraction(radiationDose, TumorCells.fullName, "radiationSensitivityOfTumorCellsAlpha", "radiationSensitivityOfTumorCellsBeta");
            double dieProbRad = 1 - TumorCells.survivingFractionT;
            double dieProbImm = TumorCells.survivingFractionT * primaryImmuneResponse;
            double divProb = TumorCells.survivingFractionT * (1 - primaryImmuneResponse) * TumorCells.tumorGrowthRate;
            return new double[]{dieProbRad, dieProbImm, divProb};
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void getTriggeringCellsProb() throws Exception
    {
        try
        {
            double volumeDamagedTumorCells = (double) DoomedCells.countRad / (DoomedCells.countRad + DoomedCells.countImm + TumorCells.count);
            double activation = Math.tanh((1 - TumorCells.survivingFractionT) * volumeDamagedTumorCells);
            //make sure survivingFractionI is accurate, so this method should be run after lymphocytes method
            TriggeringCells.survivingFractionI = Lymphocytes.survivingFractionL;
            //Lymphocytes.dieProb = 1 - Lymphocytes.survivingFractionL + (Lymphocytes.survivingFractionL * Lymphocytes.decayConstantOfL);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}

class Lymphocytes
{
    public static String name = "Lymphocyte Cells";
    public static double dieProb;
    public static double divProb = 0;
    public static int colorIndex = 0;
    public static int count;
    public static String fullName;
    public static double survivingFractionL;
    public static double decayConstantOfL;
    public static double tumorInfiltrationRate;

    public void Lymphocytes()
    {
        count = 0;
        try
        {
            fullName = "OnLattice2DCells." + OnLattice2DGrid.className;
            decayConstantOfL = CellFunctions.getDecayConstant(fullName, "decayConstantOfL");
            tumorInfiltrationRate = CellFunctions.getTumorInfiltrationRate(fullName);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}

class TumorCells
{
    public static String name = "Tumor Cells";
    public static double dieProbRad;
    public static double dieProbImm;
    public static double divProb;
    public static int colorIndex = 1;
    public static int count;
    public static String fullName;
    public static double survivingFractionT;;
    public static double tumorGrowthRate;

    public void TumorCells()
    {
        count = 0;
        try
        {
            fullName = "OnLattice2DCells." + OnLattice2DGrid.className;
            tumorGrowthRate = CellFunctions.getTumorGrowthRate(fullName);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}

class DoomedCells
{
    public static String name = "Doomed Cells";
    public static String nameRad = "Doomed Cells Rad";
    public static String nameImm = "Doomed Cells Imm";
    public static double dieProb;
    public static double divProb = 0;
    public static int colorIndex = 3;
    public static int countRad, countImm;
    public static double decayConstantOfD;

    public void DoomedCells()
    {
        countRad = 0; countImm = 0;
        try
        {
            String fullName = "OnLattice2DCells." + OnLattice2DGrid.className;
            decayConstantOfD = CellFunctions.getDecayConstant(fullName, "decayConstantOfD");
            dieProb = decayConstantOfD;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}

class TriggeringCells
{
    public static String name = "Triggering Cells";
    public static double dieProb;
    public static double divProb;
    public static int colorIndex = 6;
    public static int count;
    public static String fullName;
    public static double survivingFractionI;;

    public void TriggeringCells()
    {
        count = 0;
        try
        {
            fullName = "OnLattice2DCells." + OnLattice2DGrid.className;

        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}

abstract class Figure2 implements ModelParameters
{
    public static double radiationSensitivityOfTumorCellsAlpha = 0; //null
    public static double radiationSensitivityOfTumorCellsBeta = 0;  //null
    public static double radiationSensitivityOfLymphocytesAlpha = 0; //null
    public static double radiationSensitivityOfLymphocytesBeta = 0; //null
    public static double tumorGrowthRate = 0.217;
    public static double tumorInfiltrationRate = 0.1;
    public static double rateOfCellKilling = 0.05;
    public static double decayConstantOfD = 0.039;
    public static double decayConstantOfL = 0.335;
    public static double recoveryConstantOfA = 0.039;
    public static double radiationInducedInfiltration = 0; //null
    public static double immuneSuppressionEffect = 0.015;
}

abstract class Figure3 implements ModelParameters
{
    public static double radiationSensitivityOfTumorCellsAlpha = 0.05;
    public static double radiationSensitivityOfTumorCellsBeta = 0.0114;
    public static double radiationSensitivityOfLymphocytesAlpha = 0.182;
    public static double radiationSensitivityOfLymphocytesBeta = 0.143;
    public static double tumorGrowthRate = 0.217;
    public static double tumorInfiltrationRate = 0.05;
    public static double rateOfCellKilling = 0.135;
    public static double decayConstantOfD = 0.045;
    public static double decayConstantOfL = 0.045;
    public static double recoveryConstantOfA = 0.045;
    public static double radiationInducedInfiltration = 0; //null
    public static double immuneSuppressionEffect = 0.51;
}

abstract class Figure4 implements ModelParameters
{
    public static double radiationSensitivityOfTumorCellsAlpha = 0.05;
    public static double radiationSensitivityOfTumorCellsBeta = 0.0114;
    public static double radiationSensitivityOfLymphocytesAlpha = 0.182;
    public static double radiationSensitivityOfLymphocytesBeta = 0.143;
    public static double tumorGrowthRate = 0.217;
    public static double tumorInfiltrationRate = 0.5;
    public static double rateOfCellKilling = 0.135;
    public static double decayConstantOfD = 0.045;
    public static double decayConstantOfL = 0.045;
    public static double recoveryConstantOfA = 0.045;
    public static double radiationInducedInfiltration = 300;
    public static double immuneSuppressionEffect = 1.1;
}

abstract class Figure5 implements ModelParameters
{
    public static double radiationSensitivityOfTumorCellsAlpha = 0.05;
    public static double radiationSensitivityOfTumorCellsBeta = 0.0114;
    public static double radiationSensitivityOfLymphocytesAlpha = 0.182;
    public static double radiationSensitivityOfLymphocytesBeta = 0.143;
    public static double tumorGrowthRate = 0.217;
    public static double tumorInfiltrationRate = 0.5;
    public static double rateOfCellKilling = 0.135;
    public static double decayConstantOfD = 0.045;
    public static double decayConstantOfL = 0.045;
    public static double recoveryConstantOfA = 0.045;
    public static double radiationInducedInfiltration = 300;
    public static double immuneSuppressionEffect = 1.1;
}

abstract class Figure6 implements ModelParameters
{
    public static double radiationSensitivityOfTumorCellsAlpha = 0.214;
    public static double radiationSensitivityOfTumorCellsBeta = 0.0214;
    public static double radiationSensitivityOfLymphocytesAlpha = 0.182;
    public static double radiationSensitivityOfLymphocytesBeta = 0.143;
    public static double tumorGrowthRate = 0.03;
    public static double tumorInfiltrationRate = 0.1;
    public static double rateOfCellKilling = 0.004;
    public static double decayConstantOfD = 0.045;
    public static double decayConstantOfL = 0.056;
    public static double recoveryConstantOfA = 0.045;
    public static double radiationInducedInfiltration = 4.6;
    public static double immuneSuppressionEffect = 0.5;
}

public class OnLattice2DGrid extends AgentGrid2D<CellFunctions>
{
    Rand rng = new Rand();
    int[] divHood = Util.VonNeumannHood(false);

    public static String className = "Figure3";
    public static int baseRadiationDose;
    public static List<Integer> radiationTimesteps = List.of(500);
    public static boolean spatialRadiation = true;

    public static final String directory = "C:\\Users\\Hannah\\Documents\\HALModeling2024Outs\\";
    public static final String fileName1 = "TrialRunCounts.csv";
    public static final String fullPath1 = directory + fileName1;
    public static final String fileName2 = "TrialRunProbabilities.csv";
    public static final String fullPath2 = directory + fileName2;

    public OnLattice2DGrid(int x, int y)
    {
        super(x, y, CellFunctions.class);
    }

    public List<int[]> spatialRadiationArea(GridWindow win)
    {
        int centerX = xDim/2;
        int centerY = yDim/2;
        double targetPercentage = 0.5;

        int targetPixelsInCircle = (int) (TumorCells.count * targetPercentage);

        int radius = 0;
        for (int testRadius = 1; testRadius <= xDim; testRadius++)
        {
            int count = 0;
            for (int i = 0; i < xDim; i++)
            {
                for (int j = 0; j < yDim; j++)
                {
                    if (isInsideCircle(i, j, centerX, centerY, testRadius) && win.GetPix(i, j) == Util.CategorialColor(TumorCells.colorIndex))
                    {
                        count++;
                        if (count >= targetPixelsInCircle)
                        {
                            radius = testRadius;
                            j = yDim; i = xDim; testRadius = xDim + 1;
                        }
                    }
                }
            }
        }

        List<int[]> pixelsInCircle = new ArrayList<>();
        for (int i = 0; i < xDim; i++)
        {
            for (int j = 0; j < yDim; j++)
            {
                if (isInsideCircle(i, j, centerX, centerY, radius))
                {
                    //win.SetPix(i, j, Util.GREEN);
                    pixelsInCircle.add(new int[]{i, j});
                }
            }
        }
        return pixelsInCircle;
    }

    public static boolean isInsideCircle(int i, int j, int centerX, int centerY, int radius)
    {
        int dx = i - centerX;
        int dy = j - centerY;
        return dx * dx + dy * dy <= radius * radius;
    }

    public List<int[]> spatialRadiationApplied(GridWindow win, List<int[]> pixelsInCircle, boolean radiate) throws Exception
    {
        int radiationDose;
        double LDieProb;
        double[] values = new double[3];

        if (radiate)
        {
            radiationDose = 10;
            LDieProb = CellFunctions.getLymphocytesProb(radiationDose);
            values = CellFunctions.getTumorCellsProb(radiationDose);
        }
        else
        {
            radiationDose = baseRadiationDose;
            LDieProb = Lymphocytes.dieProb;
            values[0] = TumorCells.dieProbRad; values[1] = TumorCells.dieProbImm; values[2] = TumorCells.divProb;
        }

        for (int[] pixel : pixelsInCircle)
        {
            CellFunctions cell = GetAgent(pixel[0], pixel[1]);
            if (cell != null)
            {
                if (cell.type == CellFunctions.Type.LYMPHOCYTE)
                {
                    cell.radiationDose = radiationDose;
                    cell.dieProb = LDieProb;
                }
                else if (cell.type == CellFunctions.Type.TUMOR)
                {
                    cell.radiationDose = radiationDose;
                    cell.dieProbRad = values[0]; cell.dieProbImm = values[1]; cell.divProb = values[2];
                }
            }
        }
        return pixelsInCircle;
    }

    public void Init(GridWindow win, OnLattice2DGrid model) throws Exception
    {
        //model.NewAgentSQ(model.xDim/2, model.yDim/2).Init(TumorCells.colorIndex);
        int tumorSize = 1;
        int lymphocitePopulation = 0;
        if (tumorSize + lymphocitePopulation > model.xDim * model.yDim)
        {
            System.err.println("Error: Number of tumor and lymphocite cells exceeds grid size.\n" +
                    "Maximum Grid Capacity: " + (model.xDim * model.yDim) + " cells");
            System.exit(0);
        }

        baseRadiationDose = 0;
        Lymphocytes.dieProb = CellFunctions.getLymphocytesProb(baseRadiationDose);
        if (lymphocitePopulation > 0)
        {
            getAvailableSpaces(win, false, lymphocitePopulation);
        }

        double[] values = CellFunctions.getTumorCellsProb(baseRadiationDose);
        TumorCells.dieProbRad = values[0]; TumorCells.dieProbImm = values[1]; TumorCells.divProb = values[2];

        if (tumorSize > 0)
        {
            model.NewAgentSQ(model.xDim/2, model.yDim/2).Init(CellFunctions.Type.TUMOR);
            TumorCells.count++;
        }
        if (tumorSize > 1)
        {
            for (int i = 0; i < tumorSize; i++)
            {
                for (CellFunctions cell:this)
                {
                    cell.mapEmptyHood();
                    if (TumorCells.count == tumorSize)
                    {
                        i = tumorSize;
                        break;
                    }
                }
            }
        }
    }

    public void StepCells ()
    {
        for (CellFunctions cell:this) //this is a for-each loop, "this" refers to this grid
        {
            cell.StepCell();
        }
    }

    public List<int[]> getAvailableSpaces(GridWindow win, boolean migration, int lymphocitePopulation) throws Exception
    {
        List<int[]> availableSpaces = new ArrayList<>(); //This is a list of arrays, each array will store x- and y-coodinate
        if (TumorCells.count + DoomedCells.countRad + DoomedCells.countImm == this.xDim * this.yDim)
        {
            return availableSpaces;
        }

        for (int i = 0; i < length; i++)
        {
            CellFunctions cell = GetAgent(i);
            if (cell == null)
            {
                cell = NewAgentSQ(i);
                availableSpaces.add(new int[]{(int) cell.Xpt(),(int) cell.Ypt()});
                cell.Dispose();
            }
        }
        if (migration)
        {
            new CellFunctions().lymphociteMigration(availableSpaces, this, migration, 0); //Doing the above 2 lines in 1 line
        }
        else if (!migration)
        {
            new CellFunctions().lymphociteMigration(availableSpaces, this, migration, lymphocitePopulation);
        }
        return availableSpaces;
    }

    public void DrawModelandUpdateTumorProb(GridWindow win, GifMaker gif)
    {
        int color;
        for (int i = 0; i < length; i++)
        {
            CellFunctions cell = GetAgent(i);
            if (cell != null)
            {
                color = cell.color;
                if (cell.type == CellFunctions.Type.TUMOR)
                {
                    cell.dieProbRad = TumorCells.dieProbRad;
                    cell.dieProbImm = TumorCells.dieProbImm;
                    cell.divProb = TumorCells.divProb;
                }
            }
            else
            {
                color = Util.BLACK;
            }
            win.SetPix(i, color);
        }
        //gif.AddFrame(win);
    }

    public String findColor(int colorIndex)
    {
        if (colorIndex == 0)
        {
            return "blue";
        }
        else if (colorIndex == 1)
        {
            return "red";
        }
        else if (colorIndex == 2)
        {
            return "green";
        }
        else if (colorIndex == 3)
        {
            return "yellow";
        }
        else if (colorIndex == 4)
        {
            return "orange";
        }
        else if (colorIndex == 5)
        {
            return "cyan";
        }
        else if (colorIndex == 6)
        {
            return "pink";
        }
        else if (colorIndex == 7)
        {
            return "blue";
        }
        else if (colorIndex == 8)
        {
            return "brown";
        }
        else if (colorIndex == 9)
        {
            return "light blue";
        }
        else if (colorIndex == 10)
        {
            return "light red";
        }
        else if (colorIndex == 11)
        {
            return "light green";
        }
        else if (colorIndex == 12)
        {
            return "light yellow";
        }
        else if (colorIndex == 13)
        {
            return "light purple";
        }
        else if (colorIndex == 14)
        {
            return "light orange";
        }
        else if (colorIndex == 15)
        {
            return "light cyan";
        }
        else if (colorIndex == 16)
        {
            return "light pink";
        }
        else if (colorIndex == 17)
        {
            return "light brown";
        }
        else if (colorIndex == 18)
        {
            return "light gray";
        }
        else if (colorIndex == 19)
        {
            return "dark gray";
        }
        return "unknown color";
    }

    public void saveCountsToCSV(String fullPath1, boolean append, int timestep)
    {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fullPath1, append)))
        {
            if (timestep == 0)
            {
//                writer.write("Timestep," + Lymphocytes.name + "," + TumorCells.name + "," + DoomedCells.name);
                writer.write("Timestep, Lymphocytes, TumorCells, DoomedCellsRad, DoomedCellsImm, Lymphocytes DieProb, Tumor DieProbRad, Tumor DieProbImm, Tumor DivProb");
                writer.newLine();
            }
            //writer.write(timestep + "," + Lymphocytes.count + "," + TumorCells.count + "," + DoomedCells.count);
            writer.write(timestep + "," + Lymphocytes.count + "," + TumorCells.count + "," + DoomedCells.countRad + "," + DoomedCells.countImm + "," + Lymphocytes.dieProb + "," + TumorCells.dieProbRad + "," + TumorCells.dieProbImm + "," + TumorCells.divProb);
            writer.newLine();
        }
        catch (IOException e)
        {
            System.err.println("Failed to write CSV file: " + e.getMessage());
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void saveProbabilitiesToCSV(String fullPath2, boolean append, int timestep, GridWindow win)
    {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fullPath2, append)))
        {
            if (timestep == 0)
            {
                writer.write("Timestep, Cell, Type, Color, RadiationDose, DieProb, DieProbRad, DieProbImm, DivProb");
                writer.newLine();
            }
            for (int i = 0; i < length; i++)
            {
                OnLattice2DCells.CellFunctions cell = GetAgent(i);
                if (cell != null)
                {
                    writer.write(timestep + "," + cell + "," + cell.type + "," + cell.color + "," + cell.radiationDose + "," + cell.dieProb + "," + cell.dieProbRad + "," + cell.dieProbImm + "," + cell.divProb);
                    writer.newLine();
                }
//                else
//                {
//                    writer.write(timestep + ", ," + win.GetPix(i) + ", , , , , ");
//                }
//                writer.newLine();
            }
        }
        catch (IOException e)
        {
            System.err.println("Failed to write CSV file: " + e.getMessage());
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void printPopulation(String name, int colorIndex, int count)
    {
        System.out.println("Population of " + name + " (" + findColor(colorIndex) + "): " + count);
    }

    public static void main (String[] args) throws Exception
    {
        System.out.println(className);
        //System.out.println(className + ":\nRadiation Dose: " + radiationDose);

        int x = 8;
        int y = 8;
        int timesteps = 1000;
        GridWindow win = new GridWindow(x, y, 5);
        OnLattice2DGrid model = new OnLattice2DGrid(x, y);

        new Lymphocytes().Lymphocytes();
        new TumorCells().TumorCells();
        new DoomedCells().DoomedCells();
        new TriggeringCells().TriggeringCells();
        List<int[]> pixelsInCircle = List.of(new int[]{0, 0});

        model.Init(win, model);
        model.saveCountsToCSV(fullPath1, false, 0);
        model.saveProbabilitiesToCSV(fullPath2, false, 0, win);

        GifMaker gif = new GifMaker(directory + "TrialRunGif.gif",1,false);

        for (int i = 1; i <= timesteps; i++)
        {
            win.TickPause(1);

            if (radiationTimesteps.contains(i))
            {
                if (TumorCells.count > 2)
                {
                    pixelsInCircle = model.spatialRadiationApplied(win, model.spatialRadiationArea(win), true);
                }
            }
            else if (radiationTimesteps.contains(i - 1))
            {
                model.spatialRadiationApplied(win, pixelsInCircle, false);
            }

            model.StepCells();

            if (Lymphocytes.count < TumorCells.count)
            {
                model.getAvailableSpaces(win, true, 0); //Lymphocyte Migration
            }
            else
            {
                model.getAvailableSpaces(win, true, 0);
            }

            model.saveCountsToCSV(fullPath1, true, i);
            model.saveProbabilitiesToCSV(fullPath2, true, i, win);

            double[] values = CellFunctions.getTumorCellsProb(baseRadiationDose);
            TumorCells.dieProbRad = values[0]; TumorCells.dieProbImm = values[1]; TumorCells.divProb = values[2];
            model.DrawModelandUpdateTumorProb(win, gif); //get occupied spaces to use for stepCells method, rerun if model pop goes to 0

            if (model.Pop() == 0)
            {
                model.Init(win, model);
                model.saveCountsToCSV(fullPath1, true, 0);
                model.saveProbabilitiesToCSV(fullPath2, true, 0, win);
                i = 1;
            }
        }

        gif.Close();

        model.printPopulation(Lymphocytes.name, Lymphocytes.colorIndex, Lymphocytes.count);
        model.printPopulation(TumorCells.name, TumorCells.colorIndex, TumorCells.count);
        model.printPopulation("Doomed Cells Radiation", DoomedCells.colorIndex, DoomedCells.countRad);
        model.printPopulation("Doomed Cells Immune", DoomedCells.colorIndex, DoomedCells.countImm);
        System.out.println("Population Total: " + model.Pop());
        System.out.println("Unoccupied Spaces: " + model.getAvailableSpaces(win, false, 0).size());
        System.out.println();
    }
}