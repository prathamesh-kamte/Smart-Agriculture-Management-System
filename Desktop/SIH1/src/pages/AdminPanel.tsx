import { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { supabase } from '@/integrations/supabase/client';
import { toast } from 'sonner';
import { Users, Building2, TrendingUp, BarChart3, ArrowLeft } from 'lucide-react';
import { Link } from 'react-router-dom';

interface Analytics {
  totalStudents: number;
  totalIndustries: number;
  totalMatches: number;
  placementRate: number;
  ruralStudents: number;
  urbanStudents: number;
  generalCategory: number;
  backwardCategory: number;
  placedStudents: number;
}

interface IndustryDemand {
  sector: string;
  count: number;
}

interface Student {
  id: string;
  name: string;
  email: string;
  qualification: string;
  location: string;
  created_at: string;
}

interface Industry {
  id: string;
  company_name: string;
  role: string;
  location: string;
  internship_capacity: number;
  created_at: string;
}

interface Match {
  id: string;
  student_id: string;
  industry_id: string;
  match_score: number;
  status: string;
  created_at: string;
}

const AdminPanel = () => {
  const [analytics, setAnalytics] = useState<Analytics>({
    totalStudents: 0,
    totalIndustries: 0,
    totalMatches: 0,
    placementRate: 0,
    ruralStudents: 0,
    urbanStudents: 0,
    generalCategory: 0,
    backwardCategory: 0,
    placedStudents: 0,
  });
  const [industryDemand, setIndustryDemand] = useState<IndustryDemand[]>([]);
  const [students, setStudents] = useState<Student[]>([]);
  const [industries, setIndustries] = useState<Industry[]>([]);
  const [matches, setMatches] = useState<Match[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchAnalytics();
    fetchStudents();
    fetchIndustries();
    fetchMatches();
  }, []);

  const fetchAnalytics = async () => {
    try {
      const [studentsRes, profilesRes, industriesRes, matchesRes, internshipsRes] = await Promise.all([
        supabase.from('students').select('id'),
        supabase.from('user_profiles').select('rural_status, background').eq('user_type', 'student'),
        supabase.from('industries').select('id'),
        supabase.from('matches').select('id, status, application_status'),
        supabase.from('internships').select('sector')
      ]);

      const totalStudents = studentsRes.data?.length || 0;
      const totalIndustries = industriesRes.data?.length || 0;
      const totalMatches = matchesRes.data?.length || 0;
      const successfulMatches = matchesRes.data?.filter((m: any) => 
        m.status === 'accepted' || m.application_status === 'accepted'
      ).length || 0;
      const placementRate = totalStudents > 0 ? (successfulMatches / totalStudents) * 100 : 0;

      // Rural vs Urban split
      const ruralStudents = profilesRes.data?.filter((p: any) => p.rural_status === 'Rural').length || 0;
      const urbanStudents = profilesRes.data?.filter((p: any) => p.rural_status === 'Urban').length || 0;

      // Category-wise split
      const generalCategory = profilesRes.data?.filter((p: any) => p.background === 'General').length || 0;
      const backwardCategory = profilesRes.data?.filter((p: any) => 
        p.background === 'OBC/SC/ST/Other Backward Background'
      ).length || 0;

      // Industry demand trends
      const sectorCounts: Record<string, number> = {};
      internshipsRes.data?.forEach((i: any) => {
        if (i.sector) {
          sectorCounts[i.sector] = (sectorCounts[i.sector] || 0) + 1;
        }
      });
      const demandData = Object.entries(sectorCounts)
        .map(([sector, count]) => ({ sector, count }))
        .sort((a, b) => b.count - a.count);

      setAnalytics({
        totalStudents,
        totalIndustries,
        totalMatches,
        placementRate,
        ruralStudents,
        urbanStudents,
        generalCategory,
        backwardCategory,
        placedStudents: successfulMatches,
      });
      setIndustryDemand(demandData);
    } catch (error: any) {
      console.error('Error fetching analytics:', error);
      toast.error('Failed to fetch analytics');
    }
  };

  const fetchStudents = async () => {
    try {
      const { data, error } = await (supabase as any)
        .from('students')
        .select('id, name, email, qualification, location, created_at')
        .order('created_at', { ascending: false });

      if (error) throw error;
      setStudents(data || []);
    } catch (error: any) {
      toast.error('Failed to fetch students');
    }
  };

  const fetchIndustries = async () => {
    try {
      const { data, error } = await (supabase as any)
        .from('industries')
        .select('id, company_name, role, location, internship_capacity, created_at')
        .order('created_at', { ascending: false });

      if (error) throw error;
      setIndustries(data || []);
    } catch (error: any) {
      toast.error('Failed to fetch industries');
    }
  };

  const fetchMatches = async () => {
    try {
      const { data, error } = await (supabase as any)
        .from('matches')
        .select('id, student_id, industry_id, match_score, status, created_at')
        .order('created_at', { ascending: false });

      if (error) throw error;
      setMatches(data || []);
    } catch (error: any) {
      toast.error('Failed to fetch matches');
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-4"></div>
          <p>Loading admin panel...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary/5 to-success/5">
      <div className="container mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex justify-between items-center mb-8">
          <div>
            <h1 className="text-3xl font-bold">Admin Panel</h1>
            <p className="text-muted-foreground">Manage users, matches, and view analytics</p>
          </div>
          <Button variant="outline" asChild>
            <Link to="/">
              <ArrowLeft className="w-4 h-4 mr-2" />
              Back to Home
            </Link>
          </Button>
        </div>

        {/* Analytics Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Total Students</CardTitle>
              <Users className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{analytics.totalStudents}</div>
              <p className="text-xs text-muted-foreground">Registered students</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Total Companies</CardTitle>
              <Building2 className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{analytics.totalIndustries}</div>
              <p className="text-xs text-muted-foreground">Registered companies</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Total Matches</CardTitle>
              <TrendingUp className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{analytics.totalMatches}</div>
              <p className="text-xs text-muted-foreground">AI-generated matches</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Placement Rate</CardTitle>
              <BarChart3 className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{analytics.placementRate.toFixed(1)}%</div>
              <p className="text-xs text-muted-foreground">{analytics.placedStudents} students placed</p>
            </CardContent>
          </Card>
        </div>

        {/* Detailed Analytics */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
          <Card>
            <CardHeader>
              <CardTitle>Rural vs Urban Distribution</CardTitle>
              <CardDescription>Student demographics by location type</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium">Rural Students</span>
                  <Badge variant="outline">{analytics.ruralStudents}</Badge>
                </div>
                <div className="h-2 bg-secondary rounded-full overflow-hidden">
                  <div 
                    className="h-full bg-primary transition-all"
                    style={{ 
                      width: `${analytics.totalStudents > 0 ? (analytics.ruralStudents / analytics.totalStudents) * 100 : 0}%` 
                    }}
                  />
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium">Urban Students</span>
                  <Badge variant="outline">{analytics.urbanStudents}</Badge>
                </div>
                <div className="h-2 bg-secondary rounded-full overflow-hidden">
                  <div 
                    className="h-full bg-success transition-all"
                    style={{ 
                      width: `${analytics.totalStudents > 0 ? (analytics.urbanStudents / analytics.totalStudents) * 100 : 0}%` 
                    }}
                  />
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Category-wise Distribution</CardTitle>
              <CardDescription>Student demographics by background</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium">General Category</span>
                  <Badge variant="outline">{analytics.generalCategory}</Badge>
                </div>
                <div className="h-2 bg-secondary rounded-full overflow-hidden">
                  <div 
                    className="h-full bg-primary transition-all"
                    style={{ 
                      width: `${analytics.totalStudents > 0 ? (analytics.generalCategory / analytics.totalStudents) * 100 : 0}%` 
                    }}
                  />
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium">OBC/SC/ST/Backward</span>
                  <Badge variant="outline">{analytics.backwardCategory}</Badge>
                </div>
                <div className="h-2 bg-secondary rounded-full overflow-hidden">
                  <div 
                    className="h-full bg-success transition-all"
                    style={{ 
                      width: `${analytics.totalStudents > 0 ? (analytics.backwardCategory / analytics.totalStudents) * 100 : 0}%` 
                    }}
                  />
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Industry Demand Trends */}
        <Card className="mb-8">
          <CardHeader>
            <CardTitle>Industry Demand Trends</CardTitle>
            <CardDescription>Internship opportunities by sector</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {industryDemand.length === 0 ? (
                <p className="text-sm text-muted-foreground text-center py-4">No data available</p>
              ) : (
                industryDemand.map((item, index) => (
                  <div key={item.sector} className="flex items-center gap-3">
                    <Badge variant="outline" className="w-8 justify-center">{index + 1}</Badge>
                    <span className="flex-1 text-sm font-medium">{item.sector}</span>
                    <Badge>{item.count} internships</Badge>
                    <div className="w-32 h-2 bg-secondary rounded-full overflow-hidden">
                      <div 
                        className="h-full bg-primary transition-all"
                        style={{ 
                          width: `${industryDemand[0] ? (item.count / industryDemand[0].count) * 100 : 0}%` 
                        }}
                      />
                    </div>
                  </div>
                ))
              )}
            </div>
          </CardContent>
        </Card>

        {/* Data Tables */}
        <Tabs defaultValue="students" className="w-full">
          <TabsList className="grid w-full grid-cols-3">
            <TabsTrigger value="students">Students</TabsTrigger>
            <TabsTrigger value="industries">Companies</TabsTrigger>
            <TabsTrigger value="matches">Matches</TabsTrigger>
          </TabsList>

          <TabsContent value="students">
            <Card>
              <CardHeader>
                <CardTitle>Registered Students</CardTitle>
                <CardDescription>All students registered on the platform</CardDescription>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Name</TableHead>
                      <TableHead>Email</TableHead>
                      <TableHead>Qualification</TableHead>
                      <TableHead>Location</TableHead>
                      <TableHead>Registration Date</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {students.map((student) => (
                      <TableRow key={student.id}>
                        <TableCell className="font-medium">{student.name}</TableCell>
                        <TableCell>{student.email}</TableCell>
                        <TableCell>{student.qualification}</TableCell>
                        <TableCell>{student.location}</TableCell>
                        <TableCell>{new Date(student.created_at).toLocaleDateString()}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="industries">
            <Card>
              <CardHeader>
                <CardTitle>Registered Companies</CardTitle>
                <CardDescription>All companies offering internships</CardDescription>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Company</TableHead>
                      <TableHead>Role</TableHead>
                      <TableHead>Location</TableHead>
                      <TableHead>Capacity</TableHead>
                      <TableHead>Registration Date</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {industries.map((industry) => (
                      <TableRow key={industry.id}>
                        <TableCell className="font-medium">{industry.company_name}</TableCell>
                        <TableCell>{industry.role}</TableCell>
                        <TableCell>{industry.location}</TableCell>
                        <TableCell>{industry.internship_capacity}</TableCell>
                        <TableCell>{new Date(industry.created_at).toLocaleDateString()}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="matches">
            <Card>
              <CardHeader>
                <CardTitle>AI Matches</CardTitle>
                <CardDescription>All student-company matches generated by AI</CardDescription>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Student ID</TableHead>
                      <TableHead>Industry ID</TableHead>
                      <TableHead>Match Score</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Created Date</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {matches.map((match) => (
                      <TableRow key={match.id}>
                        <TableCell className="font-mono text-xs">{match.student_id.slice(0, 8)}...</TableCell>
                        <TableCell className="font-mono text-xs">{match.industry_id.slice(0, 8)}...</TableCell>
                        <TableCell>
                          <Badge variant={match.match_score >= 80 ? 'default' : match.match_score >= 60 ? 'secondary' : 'outline'}>
                            {match.match_score}%
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <Badge variant={match.status === 'accepted' ? 'default' : 'secondary'}>
                            {match.status}
                          </Badge>
                        </TableCell>
                        <TableCell>{new Date(match.created_at).toLocaleDateString()}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
};

export default AdminPanel;