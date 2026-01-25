import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { useNavigate } from 'react-router-dom';
import { supabase } from '@/integrations/supabase/client';
import { toast } from 'sonner';
import { User, MapPin, GraduationCap, Star, LogOut, RefreshCw, Building2, MessageSquare } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { FeedbackDialog } from '@/components/FeedbackDialog';

interface IndustryProfile {
  id: string;
  company_name: string;
  role: string;
  required_skills: string[];
  internship_capacity: number;
  location: string;
  sector: string;
}

interface StudentMatch {
  id: string;
  user_id: string;
  name: string;
  qualification: string;
  skills: string[];
  location: string;
  sector_interest: string;
  category: string;
  match_score: number;
}

const IndustryDashboard = () => {
  const navigate = useNavigate();
  const { user, session, signOut } = useAuth();
  const [industryProfile, setIndustryProfile] = useState<IndustryProfile | null>(null);
  const [matches, setMatches] = useState<StudentMatch[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [feedbackDialogOpen, setFeedbackDialogOpen] = useState(false);
  const [selectedStudentForFeedback, setSelectedStudentForFeedback] = useState<any>(null);

  useEffect(() => {
    if (!user || !session) {
      navigate('/industry/auth');
      return;
    }

    // Set mock data immediately
    setIndustryProfile({
      id: 'mock-industry-id',
      company_name: 'Acme Corp',
      role: 'employer',
      required_skills: ['React', 'Python', 'JavaScript', 'Node.js'],
      internship_capacity: 5,
      location: 'San Francisco',
      sector: 'Tech'
    });

    setMatches([
      {
        id: 'mock-match-1',
        user_id: 'mock-student-1',
        name: 'Alex Johnson',
        qualification: 'Computer Science',
        skills: ['React', 'Python', 'JavaScript'],
        location: 'New York',
        sector_interest: 'Tech',
        category: 'frontend',
        match_score: 95
      },
      {
        id: 'mock-match-2',
        user_id: 'mock-student-2',
        name: 'Sarah Chen',
        qualification: 'Software Engineering',
        skills: ['Node.js', 'MongoDB', 'Express'],
        location: 'San Francisco',
        sector_interest: 'Tech',
        category: 'backend',
        match_score: 88
      }
    ]);

    setIsLoading(false);
  }, [user, session, navigate]);

  const refreshMatches = () => {
    // Mock refresh - just show a toast
    toast.success('Matches refreshed!');
  };

  const handleLogout = async () => {
    await signOut();
    navigate('/');
  };

  if (!industryProfile) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-primary/5 to-success/5 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-4"></div>
          <p className="text-muted-foreground">Loading your company profile...</p>
        </div>
      </div>
    );
  }

  const Label = ({ children }: { children: React.ReactNode }) => (
    <span className="text-sm font-medium text-muted-foreground">{children}:</span>
  );

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary/5 to-success/5 p-4">
      <div className="max-w-7xl mx-auto">
        <div className="flex justify-between items-center mb-8">
          <div>
            <h1 className="text-3xl font-bold">Welcome, {industryProfile.company_name}!</h1>
            <p className="text-muted-foreground">Your talent acquisition dashboard</p>
          </div>
          <Button onClick={handleLogout} variant="outline" size="sm">
            <LogOut className="w-4 h-4 mr-2" />
            Logout
          </Button>
        </div>

        {/* Company Profile Section */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
          <Card className="lg:col-span-1">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Building2 className="w-5 h-5" />
                Company Profile
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="flex items-center gap-2">
                  <User className="w-4 h-4 text-muted-foreground" />
                  <Label>Role</Label>
                  <span className="font-medium">{industryProfile.role}</span>
                </div>
                <div className="flex items-center gap-2">
                  <MapPin className="w-4 h-4 text-muted-foreground" />
                  <Label>Location</Label>
                  <span className="font-medium">{industryProfile.location}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Star className="w-4 h-4 text-muted-foreground" />
                  <Label>Sector</Label>
                  <span className="font-medium">{industryProfile.sector}</span>
                </div>
                <div className="flex items-center gap-2">
                  <GraduationCap className="w-4 h-4 text-muted-foreground" />
                  <Label>Capacity</Label>
                  <span className="font-medium">{industryProfile.internship_capacity} interns</span>
                </div>
                <div>
                  <div className="mb-2"><Label>Required Skills</Label></div>
                  <div className="flex flex-wrap gap-2">
                    {industryProfile.required_skills?.map((skill) => (
                      <Badge key={skill} variant="secondary">
                        {skill}
                      </Badge>
                    ))}
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="lg:col-span-2">
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="flex items-center gap-2">
                    <User className="w-5 h-5" />
                    Matched Students
                  </CardTitle>
                  <CardDescription>
                    AI-recommended students based on your requirements
                  </CardDescription>
                </div>
                <Button onClick={refreshMatches} variant="outline" size="sm" disabled={isLoading}>
                  <RefreshCw className={`w-4 h-4 mr-2 ${isLoading ? 'animate-spin' : ''}`} />
                  Refresh
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              {isLoading ? (
                <div className="text-center py-8">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-4"></div>
                  <p className="text-muted-foreground">Finding the best student matches...</p>
                </div>
              ) : matches.length === 0 ? (
                <div className="text-center py-8">
                  <User className="w-12 h-12 text-muted-foreground mx-auto mb-4" />
                  <p className="text-muted-foreground">No student matches found yet. Try refreshing!</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {matches.map((match) => (
                    <Card key={match.user_id} className="border-l-4 border-l-primary">
                      <CardHeader className="pb-3">
                        <div className="flex items-start justify-between">
                          <div className="flex-1">
                            <CardTitle className="text-lg">{match.name}</CardTitle>
                            <CardDescription className="flex items-center gap-2 mt-1">
                              <span>{match.qualification}</span>
                              <span>•</span>
                              <MapPin className="w-3 h-3" />
                              <span>{match.location}</span>
                            </CardDescription>
                          </div>
                          <div className="text-right">
                            <div className="flex items-center gap-2 mb-2">
                              <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                              <span className="font-bold text-lg">{match.match_score}%</span>
                            </div>
                            <Progress value={match.match_score} className="w-20" />
                          </div>
                        </div>
                      </CardHeader>
                      <CardContent className="pt-0">
                        <div className="space-y-3">
                          <div>
                            <div className="text-xs font-medium text-muted-foreground">Skills</div>
                            <div className="flex flex-wrap gap-1 mt-2">
                              {Array.isArray(match.skills) && match.skills.length > 0 ? (
                                match.skills.map((skill) => (
                                  <Badge key={skill} variant="outline" className="text-xs">
                                    {skill}
                                  </Badge>
                                ))
                              ) : (
                                <span className="text-xs text-muted-foreground">No skills listed</span>
                              )}
                            </div>
                          </div>
                           <div className="flex items-center justify-between">
                             <div className="flex items-center gap-4 text-sm text-muted-foreground">
                               <span className="flex items-center gap-1">
                                 <GraduationCap className="w-3 h-3" />
                                 {match.sector_interest}
                               </span>
                               {match.category && (
                                 <Badge variant="outline" className="text-xs">
                                   {match.category.toUpperCase()}
                                 </Badge>
                               )}
                             </div>
                             <div className="flex gap-2">
                               <Button 
                                 size="sm"
                                 variant="outline"
                                 onClick={() => {
                                   setSelectedStudentForFeedback(match);
                                   setFeedbackDialogOpen(true);
                                 }}
                               >
                                 <MessageSquare className="w-4 h-4 mr-1" />
                                 Feedback
                               </Button>
                               <Button size="sm">
                                 Contact Student
                               </Button>
                             </div>
                           </div>
                        </div>
                      </CardContent>
                    </Card>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
         </div>
       </div>
       
       {/* Feedback Dialog */}
       {selectedStudentForFeedback && (
         <FeedbackDialog
           open={feedbackDialogOpen}
           onOpenChange={setFeedbackDialogOpen}
           matchId={selectedStudentForFeedback.id}
           userType="industry"
           recipientName={selectedStudentForFeedback.name}
         />
       )}
     </div>
   );
 };
 
 export default IndustryDashboard;